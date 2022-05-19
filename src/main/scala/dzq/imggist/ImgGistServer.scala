package dzq.imggist

import java.net.{InetAddress, ServerSocket}
import org.apache.commons.logging.LogFactory
import dzq.utils.ArgumentParse
import dzq.imggist.handler.ImgGistRunable

object ImgGistServer {
  def main(args: Array[String]): Unit = {
    val log = LogFactory.getLog(this.getClass)

    val cfg = new ArgumentParse(args)
    val sLocalHost = cfg.getString("host", "127.0.0.1")
    val iSvrPort = cfg.getInt("port", 52402)
    log.info(s"start service: $sLocalHost:$iSvrPort")

    val svrAddress = InetAddress.getByName(sLocalHost)
    val server = new ServerSocket(iSvrPort, 10, svrAddress)

    while (true) {
      val socket = server.accept()
      socket.setSoTimeout(5 * 1000)

      val thread = new Thread(new ImgGistRunable(socket))

      thread.start()
    }

  }
}