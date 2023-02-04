package it.pps.ddos.storage.tusow

//#import


import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import scala.io.Source
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.ConnectionContext
import akka.http.scaladsl.Http
import akka.http.scaladsl.HttpsConnectionContext
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.pki.pem.DERPrivateKeyLoader
import akka.pki.pem.PEMDecoder
import com.typesafe.config.ConfigFactory
import it.pps.ddos.deployment.Deployer
import it.pps.ddos.storage.tusow.TusowAkkaService
import it.pps.ddos.storage.tusow.client.Client
import it.unibo.coordination.tusow.grpc.TusowServiceHandler

import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Success

object Server:

    trait ServerCommand
    case class Start(tusowAkkaService: TusowAkkaService) extends ServerCommand
    case class Stop() extends ServerCommand

    private val CLUSTER_NAME = "ClusterSystem"

    /**
     * Starts a TuSoW server on the default cluster
     */
    def start(): Unit =
        val conf = ConfigFactory.parseString("akka.http.server.preview.enable-http2 = on")
          .withFallback(ConfigFactory.defaultApplication())
        val system = ActorSystem[Any](Behaviors.receive((ctx, message) => {
            message match {
                case Start(service) =>
                    new Server(ctx.system.classicSystem, service).run()
                    Behaviors.same
                case Stop => Behaviors.stopped
            }
        }), CLUSTER_NAME, conf)
        system.ref ! Start(new TusowAkkaService(system))

/**
 * Wrapper class for the Akka HTTP server and the TuSoW service
 * @param system actor system on which the server will run
 * @param tusowAkkaService TuSoW service implementation
 */
class Server(system: akka.actor.ActorSystem, tusowAkkaService: TusowAkkaService):

    private val IP = "127.0.0.1"
    private val HTTP_PORT = 8080

    private def run(): Future[Http.ServerBinding] =
        implicit val sys = system
        implicit val ec: ExecutionContext = system.dispatcher

        val service: HttpRequest => Future[HttpResponse] =
            TusowServiceHandler(tusowAkkaService)

        val bound: Future[Http.ServerBinding] = Http()
          .newServerAt(interface = IP, port = HTTP_PORT)
          .bind(service)
          .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))

        bound.onComplete {
            case Success(binding) =>
                val address = binding.localAddress
                print("gRPC server bound to {}:{}\n", address.getHostString, address.getPort)
            case Failure(ex) =>
                print("Failed to bind gRPC endpoint, terminating system\n", ex)
                system.terminate()
        }
        bound