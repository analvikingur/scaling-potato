package Utils

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.elementList

import akka.actor._
import Parser._

object Utils {
    val root = "http://slova.org.ru"

    val browser = JsoupBrowser()

    def parsePoems(): Unit = {
        val mainDoc = browser get root

        val columns = mainDoc >> elementList(".list_columns1")

        val authorLinks = columns flatMap { column =>
            column >> elementList("a") map { a => prefixWithRoot(a attr "href") }
        }

        val system = ActorSystem("PiSystem")

        val listener = system.actorOf(Props[Listener], name = "listener")
        val master = system.actorOf(Props(new Parser(1500, listener)), name = "master")

        master ! StartLinksParsing(authorLinks)
    }


    def prefixWithRoot(to: String): String = {
        root + "/" + to
    }

}