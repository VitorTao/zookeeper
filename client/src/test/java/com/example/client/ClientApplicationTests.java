package com.example.client;

import com.example.client.config.ZkLock;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ClientApplicationTests {
  @Autowired
  private ZooKeeper zkClient;
  @Autowired
  private CuratorFramework curatorFramework;
  @Test
  void contextLoads() throws Exception {

    Stat stat = curatorFramework.setData()
            .withVersion(-1)
            .forPath("/node10/child_01", "I love you".getBytes());
    System.out.println("=====>修改之后的版本为：" + stat.getVersion());
  }

}
