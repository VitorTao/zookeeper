package com.example.client.config;


import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class ZkLock implements Lock {

  private String root = "/locks";
  private String lockName ;
  private String myNode ;
  private String preNode ;
  private String splitstr = "_lock_";
  private CountDownLatch latch ;
  private ZooKeeper zkClient;
//  锁可重入设计
  private volatile Thread curthread;
  private AtomicInteger lockcount = new AtomicInteger();

  public ZkLock(ZooKeeper zkClient,String lockName) throws KeeperException, InterruptedException {
    this.zkClient = zkClient;
    this.lockName = lockName;
    Stat stat = zkClient.exists(root, false);
    if(stat == null){
      zkClient.create(root, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
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
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (KeeperException e) {
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
        myNode = zkClient.create(root+"/"+lockName+splitstr, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
      }
      List<String> children = zkClient.getChildren(root, false);
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
    } catch (KeeperException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return false;
  }


  @Override
  public boolean tryLock(long time,  TimeUnit unit) throws InterruptedException {
    return false;
  }
  private boolean waitForLock(String preNode, long waitTime) throws InterruptedException, KeeperException {
    this.latch = new CountDownLatch(1);
    Stat stat = zkClient.exists(root + "/" + preNode,new WatcherApi(latch));
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
      zkClient.delete(myNode,-1);
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
