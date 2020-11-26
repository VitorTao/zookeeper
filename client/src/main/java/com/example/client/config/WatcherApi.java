package com.example.client.config;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class WatcherApi implements Watcher {

  private static final Logger logger = LoggerFactory.getLogger(WatcherApi.class);
  private CountDownLatch latch;
  public WatcherApi(CountDownLatch latch){
    this.latch = latch;
  }
  @Override
  public void process(WatchedEvent event) {
    Event.KeeperState state = event.getState();
    String path = event.getPath();
    Event.EventType type = event.getType();
    if(state == Event.KeeperState.SyncConnected) {
      if (type == Event.EventType.NodeCreated) {
        System.out.println("节点" + path + "被创建");
      } else if (type == Event.EventType.NodeDataChanged) {
        System.out.println("节点" + path + "被修改");
      } else if (type == Event.EventType.NodeDeleted) {
        System.out.println("节点" + path + "被删除");
        latch.countDown();
      } else if (type == Event.EventType.None) {

      }
    }
//    logger.info("【Watcher监听事件】={}",event.getState());
//    logger.info("【监听路径为】={}",event.getPath());
//    logger.info("【监听的类型为】={}",event.getType()); //  三种监听类型： 创建，删除，更新
  }
}