package com.example.client.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;

public class CuratorDemo {


  public static void main(String[] args) throws Exception {

    String connectionString = "192.168.33.133:2181";
    ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3,Integer.MAX_VALUE);

    CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient(connectionString, retryPolicy);
    curatorFramework.start();

    //=========================创建节点=============================

    /**
     * 创建一个 允许所有人访问的 持久节点
     */
    curatorFramework.create()
            .creatingParentsIfNeeded()//递归创建,如果没有父节点,自动创建父节点
            .withMode(CreateMode.PERSISTENT)//节点类型,持久节点
            .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)//设置ACL,和原生API相同
            .forPath("/node10/child_01","123456".getBytes());
  }
}
