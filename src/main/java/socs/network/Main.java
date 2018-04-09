package socs.network;

import java.net.InetAddress;
import java.net.UnknownHostException;

import socs.network.node.Router;
import socs.network.node.RouterRequestListener;
import socs.network.util.Configuration;

public class Main {

  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("usage: program conf_path");
      System.exit(1);
    }
    Router r = new Router(new Configuration(args[0]));
    Thread rtListener = new Thread(new RouterRequestListener(r));
    rtListener.start();
    r.terminal();
  }
}
