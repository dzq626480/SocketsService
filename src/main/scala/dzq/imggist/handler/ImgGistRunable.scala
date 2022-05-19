package dzq.imggist.handler

import com.google.protobuf.ByteString
import dzq.imaggist.pb.ImgGistPB
import org.apache.commons.logging.LogFactory
import org.openimaj.image.{FImage, ImageUtilities}
import org.openimaj.image.feature.global.Gist

import java.io.OutputStream
import scala.collection.JavaConverters._
import java.net.Socket


class ImgGistRunable(socket: Socket) extends Runnable {
  private val log = LogFactory.getLog(this.getClass)
  val sClientAddr: String = socket.getInetAddress.getHostAddress

  override def run(): Unit = {

    var gistFeatures: java.lang.Iterable[java.lang.Float] = null

    // read data from socket
    val inputStream = socket.getInputStream
    if (inputStream == null) {
      log.warn(s"not found req data, req from $sClientAddr")
      socketOutput(ImgGistPB.ImgGistReq.newBuilder().build(), gistFeatures, ImgGistPB.ImgGistResp.ImgGistRespReturnCode.IsNullRequest)
      return
    }

    // pb parse
    val req: ImgGistPB.ImgGistReq = try {
      ImgGistPB.ImgGistReq.parseFrom(ByteString.readFrom(inputStream))
    } catch {
      case e: Exception =>
        log.error(s"pb parse fail, from $sClientAddr")
        if (inputStream != null) inputStream.close()
        socketOutput(ImgGistPB.ImgGistReq.newBuilder().build(), gistFeatures, ImgGistPB.ImgGistResp.ImgGistRespReturnCode.ParsePBFailFromRequest)
        return
    }

    // gist
    gistFeatures = try {
      val gist = new Gist[FImage](64, 64)
      val oFImage: FImage = ImageUtilities.readF(req.getImageData.newInput())
      gist.analyseImage(oFImage)
      gist.getResponse.values
        .map(_.asInstanceOf[java.lang.Float]).toIterable.asJava
    } catch {
      case e:Exception =>
        log.error(s"gist analyse fail, from $sClientAddr, req id: ${req.getId}, req url: ${req.getImageUrl}")
        if (inputStream != null) inputStream.close()
        socketOutput(ImgGistPB.ImgGistReq.newBuilder().build(), gistFeatures, ImgGistPB.ImgGistResp.ImgGistRespReturnCode.GetGistDescFail)
        return
    }

    // resp
    socketOutput(req, gistFeatures)

    // exist
    if (inputStream != null) inputStream.close()
    socket.shutdownOutput()
  }

  def socketOutput(req: ImgGistPB.ImgGistReq, gfs: java.lang.Iterable[java.lang.Float], respRet:ImgGistPB.ImgGistResp.ImgGistRespReturnCode = ImgGistPB.ImgGistResp.ImgGistRespReturnCode.ReturnOK): Unit = {
    var outputStream: OutputStream = null

    // make respone
    try {
      val resp = ImgGistPB.ImgGistResp.newBuilder()
        //.setReq(req.toBuilder.clearImageData().build())
        .addAllFeatures(gfs)
        .setRet(respRet)
        .build()

      // write data to socket
      outputStream = socket.getOutputStream
      outputStream.write(resp.toByteArray)
      outputStream.flush()
    } catch {
      case e: Exception =>
        log.error(s"socket output error, from $sClientAddr, req id: ${req.getId}, req url: ${req.getImageUrl}")
    }

    if (outputStream != null) outputStream.close()
  }
}