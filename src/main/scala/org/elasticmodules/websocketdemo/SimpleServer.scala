/*
 * Copyright 2014 Elastic Modules Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticmodules.websocketdemo

import akka.actor._
import akka.io.IO
import spray.can.Http
import spray.can.server.UHttp
import spray.can.websocket
import spray.can.websocket.frame.{BinaryFrame, TextFrame}
import spray.can.websocket.FrameCommandFailed
import spray.json._
import DefaultJsonProtocol._
import spray.routing.HttpServiceActor

// !!! IMPORTANT, else `convertTo` and `toJson` won't work correctly

case class Data(page: Int, limit: Int, start: Int)
case class ReadEvent(data: Data)
case class User(id: Int, name: String, age: Int)

trait Request
case class WsRead(data: Data) extends Request


//{"event":"read","data":{"page":1,"limit":25,"start":0}}
object WsJsonProtocol extends DefaultJsonProtocol {

  implicit object WsReadJsonFormat extends RootJsonFormat[WsRead] {
    def write(p: WsRead) = JsObject(
      "event" -> JsString("read"),
      "data" -> dataFormat.write(p.data)
    )

    def read(value: JsValue) = value match {
      case JsObject(fields) =>
        (fields("event"), fields("data")) match {
          case (JsString("read"), element) =>
            WsRead(dataFormat.read(element))
          case _ => throw new DeserializationException("Unhandled Pageable")
        }
      case _ => throw new DeserializationException("Pageable Expected")
    }
  }

  implicit val dataFormat = jsonFormat3(Data)
}

object SimpleServer extends App with MySslConfiguration {

  final case class Push(msg: String)

  object WebSocketServer {
    def props() = Props(classOf[WebSocketServer])
  }

  class WebSocketServer extends Actor with ActorLogging {
    def receive = {
      // when a new connection comes in we register a WebSocketConnection actor as the per connection handler
      case Http.Connected(remoteAddress, localAddress) =>
        val serverConnection = sender()
        val conn = context.actorOf(WebSocketWorker.props(serverConnection))
        serverConnection ! Http.Register(conn)
    }
  }

  object WebSocketWorker {
    def props(serverConnection: ActorRef) = Props(classOf[WebSocketWorker], serverConnection)
  }

  class WebSocketWorker(val serverConnection: ActorRef) extends HttpServiceActor with websocket.WebSocketServerConnection {

    import WsJsonProtocol._

    override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

    def businessLogic: Receive = {
      // just bounce frames back for Autobahn testsuite
      case x@(_: BinaryFrame) =>
        sender() ! x

      case x@(_: TextFrame) =>
        val obj = x.payload.utf8String.parseJson.convertTo[WsRead]
        self ! obj

      case Push(msg) =>
        send(TextFrame(msg))

      case x: FrameCommandFailed =>
        log.error("frame command failed", x)

      case x: WsRead =>
        log.debug("Got ReadEvent: {}", x)
        send(TextFrame("""{ "event": "read", "data": [ { "id": "1", "name": "Brian", "age": "47"} ] }"""))

      case UHttp.Upgraded =>
        self ! WsRead(Data(1,25,0))
    }

    def businessLogicNoUpgrade: Receive = {
      implicit val refFactory: ActorRefFactory = context
      runRoute {
        getFromResourceDirectory("webapp")
      }
    }
  }

  def doMain() {
    implicit val system = ActorSystem()

    val server = system.actorOf(WebSocketServer.props(), "websocket")

    IO(UHttp) ! Http.Bind(server, "localhost", 8080)

    readLine("Hit ENTER to exit ...\n")
    system.shutdown()
    system.awaitTermination()
  }

  // because otherwise we get an ambiguous implicit if doMain is inlined
  doMain()
}



