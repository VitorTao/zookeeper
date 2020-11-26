package com.example.client.config;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Component
public class ZkUtil {

  private static final Logger logger = LoggerFactory.getLogger(ZkUtil.class);
  private volatile int incre = 0;
  private CountDownLatch countDownLatch;
  @Autowired
  private ZooKeeper zkClient;


  /**
   * 判断指定节点是否存在
   * @param path
   * @param needWatch  指定是否复用zookeeper中默认的Watcher
   * @return
   */
  public Stat exists(String path, boolean needWatch){
    try {
      return zkClient.exists(path,needWatch);
    } catch (Exception e) {
      logger.error("【断指定节点是否存在异常】{},{}",path,e);
      return null;
    }
  }

  /**
   *  检测结点是否存在 并设置监听事件
   *      三种监听类型： 创建，删除，更新
   *
   * @param path
   * @param watcher  传入指定的监听类
   * @return
   */
  public Stat exists(String path, Watcher watcher ){
    try {
      return zkClient.exists(path,watcher);
    } catch (Exception e) {
      logger.error("【断指定节点是否存在异常】{},{}",path,e);
      return null;
    }
  }

  /**
   * 创建持久化节点
   * @param path
   * @param data
   */
  public boolean createNode(String path, String data){
    try {
      zkClient.create(path,data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      return true;
    } catch (Exception e) {
      logger.error("【创建持久化节点异常】{},{},{}",path,data,e);
      return false;
    }
  }
  /**
   * 创建临时顺序节点
   * @param path
   * @param data
   */
  public boolean createESNode(String path, String data){
    try {
      zkClient.create(path,data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
      return true;
    } catch (Exception e) {
      logger.error("【创建持久化节点异常】{},{},{}",path,data,e);
      return false;
    }
  }


  /**
   * 修改持久化节点
   * @param path
   * @param data
   */
  public boolean updateNode(String path, String data){
    try {
      //zk的数据版本是从0开始计数的。如果客户端传入的是-1，则表示zk服务器需要基于最新的数据进行更新。如果对zk的数据节点的更新操作没有原子性要求则可以使用-1.
      //version参数指定要更新的数据的版本, 如果version和真实的版本不同, 更新操作将失败. 指定version为-1则忽略版本检查
      zkClient.setData(path,data.getBytes(),-1);
      return true;
    } catch (Exception e) {
      logger.error("【修改持久化节点异常】{},{},{}",path,data,e);
      return false;
    }
  }

  /**
   * 删除持久化节点
   * @param path
   */
  public boolean deleteNode(String path){
    try {
      //version参数指定要更新的数据的版本, 如果version和真实的版本不同, 更新操作将失败. 指定version为-1则忽略版本检查
      zkClient.delete(path,-1);
      return true;
    } catch (Exception e) {
      logger.error("【删除持久化节点异常】{},{}",path,e);
      return false;
    }
  }

  /**
   * 获取当前节点的子节点(不包含孙子节点)
   * @param path 父节点path
   */
  public List<String> getChildren(String path) throws KeeperException, InterruptedException{
    List<String> list = zkClient.getChildren(path, false);
    return list;
  }
  /**
   * 获取当前节点的子节点(不包含孙子节点)
   * @param path 父节点path
   */
  public List<String> getChildren(String path,Watcher watcher) throws KeeperException, InterruptedException{
    List<String> list = zkClient.getChildren(path, watcher);
    return list;
  }

  /**
   * 获取指定节点的值
   * @param path
   * @return
   */
  public  String getData(String path,Watcher watcher){
    try {
      Stat stat=new Stat();
      byte[] bytes=zkClient.getData(path,watcher,stat);
      return  new String(bytes);
    }catch (Exception e){
      e.printStackTrace();
      return  null;
    }
  }


  /**
   * 测试方法  初始化
   */
  @PostConstruct
  public  void init() throws KeeperException, InterruptedException {
//    String path="/zk-watcher-2";
//    logger.info("【执行初始化测试方法。。。。。。。。。。。。】");
//    createNode(path,"测试");
//    exists(path,new WatcherApi());
//    updateNode(path,"更新");
//    deleteNode(path);
//    String value=getData(path,new WatcherApi());
//    List<String> children = getChildren(path, new WatcherApi());
//    children.forEach(c -> System.out.println(c));
//    logger.info("【执行初始化测试方法getData返回值。。。。。。。。。。。。】={}",value);
    // 删除节点出发 监听事件
//    deleteNode(path);
//    long t1 = System.currentTimeMillis();
//    countDownLatch = new CountDownLatch(100);
//    lockRunable lockRunable = new lockRunable(incre,countDownLatch);
//    for (int i = 0; i < 100; i++) {
//      new Thread(lockRunable).start();
//    }
//    countDownLatch.await();
//    long t2 = System.currentTimeMillis();
//    System.out.println("耗时："+(t2-t1)/1000);
  }

  class lockRunable implements Runnable{

    private int incre;
    private CountDownLatch countDownLatch;
    private lockRunable(int incre,CountDownLatch countDownLatch){
      this.incre = incre;
      this.countDownLatch = countDownLatch;
    }
    @Override
    public void run() {
      ZkLock zkLock = null;
      try {
        System.out.println("zkclient"+zkClient);
        zkLock = new ZkLock(zkClient,"test");
      } catch (KeeperException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      zkLock.lock();
      System.out.println(Thread.currentThread()+"得到锁");
      for (int i = 0; i < 1000; i++) {
        incre++;
        System.out.println(incre);
      }

      zkLock.unlock();
      countDownLatch.countDown();
    }
  }
}
