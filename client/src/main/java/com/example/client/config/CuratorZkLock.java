package com.example.client.config;


import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class CuratorZkLock implements Lock {

  private String root = "/locks";
  private String lockName ;
  private String myNode ;
  private String preNode ;
  private String splitstr = "_lock_";
  private CountDownLatch latch ;
  private CuratorFramework zkClient;
//  锁可重入设计
  private volatile Thread curthread;
  private AtomicInteger lockcount = new AtomicInteger(0);

  public CuratorZkLock(CuratorFramework zkClient, String lockName) throws Exception {
    this.zkClient = zkClient;
    this.lockName = lockName;
    Stat stat = zkClient.checkExists().forPath(root);
    if(stat == null){
      zkClient.create().withMode(CreateMode.PERSISTENT).forPath(root);
    }
  }


  @Override
  public void lock() {
    if(tryLock()){
      System.out.println("当前线程"+Thread.currentThread()+"node:"+myNode);
      this.curthread = Thread.currentThread();
      lockcount.incrementAndGet();
    }else{
      try {
        waitForLock(preNode,10);
        //当wait 监听的前一个节点非正常删除 （断开连接），导致结束阻塞，此时需要重新获取锁流程，因为已经有节点，所以判断当前节点不为空，不用创建新节点，
//        当前节点和childrens 比较 是不是最小节点，又能回到之前的流程里
        lock();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {

  }

  @Override
  public boolean tryLock() {
    try {
      if(myNode == null){
        myNode = zkClient.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(root+"/"+lockName+splitstr) ;
      }
      List<String> children = zkClient.getChildren().forPath(root);
      List<String> lockNamechildrens = new ArrayList();
      children.forEach( c -> {
        if(c.split(splitstr)[0].equals(lockName)){
          lockNamechildrens.add(c);
        }
      });
      Collections.sort(lockNamechildrens);
      if(!lockNamechildrens.isEmpty() && myNode.equals(root+"/"+lockNamechildrens.get(0))){
        return true;
      }
      //当前节点没抢到，把前一个节点找出来
      String subMyNode = myNode.substring(myNode.lastIndexOf("/")+1);
      int mynodeindex = Collections.binarySearch(lockNamechildrens, subMyNode);
      preNode = lockNamechildrens.get(mynodeindex - 1);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }


  @Override
  public boolean tryLock(long time,  TimeUnit unit) throws InterruptedException {
    return false;
  }
  private boolean waitForLock(String preNode, long waitTime) throws Exception {
    this.latch = new CountDownLatch(1);
//    构建curator 监听器
    NodeCache nodeCache = new NodeCache(zkClient,root + "/" + preNode);
    nodeCache.getListenable().addListener(new NodeCacheListener(){
      @Override
      public void nodeChanged() throws Exception {
        latch.countDown();
      }
    });
    nodeCache.start();
    Stat stat = zkClient.checkExists().forPath(root + "/" + preNode);
    //判断比自己小一个数的节点是否存在,如果不存在则无需等待锁,同时注册监听
    if(stat != null){
      System.out.println(Thread.currentThread() + " waiting for " + root + "/" + preNode);
//      this.latch.await(waitTime, TimeUnit.SECONDS);
      this.latch.await();
      System.out.println(Thread.currentThread()+"结束阻塞");
      this.latch = null;
    }
    return true;
  }
  @Override
  public void unlock() {
    try {
      //guaranteed()保障机制，若未删除成功，只要会话有效会在后台一直尝试删除
      zkClient.delete().guaranteed().forPath(myNode);
//      zkClient.close();
      System.out.println(Thread.currentThread()+"释放锁");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  @Override
  public Condition newCondition() {
    return null;
  }
}
