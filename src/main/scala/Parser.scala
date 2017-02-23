package Parser

import java.io.{File, PrintWriter}

import akka.actor._
import akka.routing.RoundRobinPool
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.elementList
import Utils.Utils

case class StartLinksParsing(authorLinks: List[String])

case class FetchPoemLinks(authorLink: String, browser: JsoupBrowser)

case class PoemLinksResult(result: List[String])

case class StartPoemParsing(poemLinks: List[String])

case class FetchPoem(link: String, browser: JsoupBrowser)

case class PoemResult(result: String)

case class PoemsFetched(result: List[String])

class Listener extends Actor {
    def receive = {
        case PoemsFetched(result) =>

            result.zipWithIndex foreach {
                case (poem, index) =>
                    val file = new File(s"poem_$index.txt")
                    val pw = new PrintWriter(file, "UTF-8")
                    pw.write(poem)
                    pw.close()
            }

            context.system.terminate()
    }
}

class Parser(num_workers: Int, listener: ActorRef) extends Actor {
    val browser = JsoupBrowser()

    val workerRouter = context.actorOf(
        Props[ParserWorker].withRouter(RoundRobinPool(num_workers)), name = "workerRouter")

    var poemLinks: List[String] = List.empty[String]
    var numPoemLinks: Int = _
    var numPoemLinksReceived: Int = 0

    var poems: List[String] = List.empty[String]
    var numPoems: Int = _
    var numPoemsReceived: Int = 0

    def receive = {

        case StartLinksParsing(authorLinks) =>
            numPoemLinks = authorLinks.length

            authorLinks foreach { link =>
                workerRouter ! FetchPoemLinks(link, browser)
            }

        case PoemLinksResult(result) =>
            poemLinks ++= result
            numPoemLinksReceived += 1

            if (numPoemLinksReceived == numPoemLinks) {
                self ! StartPoemParsing(poemLinks)
            }

        case StartPoemParsing(poemsLinks) =>
            numPoems = poemsLinks.length

            poemsLinks foreach { link =>
                workerRouter ! FetchPoem(link, browser)
            }

        case PoemResult(result) =>
            poems ++= Array(result)
            numPoemsReceived += 1

            if (numPoemsReceived == numPoems) {
                listener ! PoemsFetched(poems)

                context.stop(self)
            }

    }

}

class ParserWorker extends Actor {
    def receive = {

        case FetchPoemLinks(authorLink, browser) =>
            val document = browser get authorLink
            val column = document >> element(".list_columns2")
            val hrefs = column >> elementList("a") map { a => Utils.prefixWithRoot(a attr "href") }

            sender ! PoemLinksResult(hrefs)

        case FetchPoem(link, browser) =>
            val document = browser get link
            val column = document >> element(".list_columns2")
            val poem = column >> text("pre")

            sender ! PoemResult(poem)
    }
}