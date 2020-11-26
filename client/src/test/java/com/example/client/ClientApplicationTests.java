package com.example.client;

import com.example.client.config.ZkLock;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ClientApplicationTests {
  @Autowired
  private ZooKeeper zkClient;
  @Test
  void contextLoads() throws KeeperException, InterruptedException {

    ZkLock zkLock = new ZkLock(zkClient,"test");
    zkLock.lock();
    System.out.println("得到锁");
    zkLock.unlock();
  }

}
