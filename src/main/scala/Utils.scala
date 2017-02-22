package Utils

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model.{Document, Element}
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.elementList

import java.io._

object Utils {
    val root = "http://slova.org.ru"

    val browser = JsoupBrowser()

    def parsePoems(): Unit = {
        val mainDoc = browser get root

        val columns = mainDoc >> elementList(".list_columns1")

        val authorLinks = columns flatMap { column =>
            column >> elementList("a") map {a => prefixWithRoot(a attr "href")}
        }

        val poemLinks = authorLinks flatMap { authorLink =>
            val document = browser get authorLink
            val column = document >> element(".list_columns2")
            column >> elementList("a") map {a => prefixWithRoot(a attr "href")}
        }

        val poems = poemLinks.par map { poemLink =>
            val document = browser get poemLink
            val column = document >> element(".list_columns2")
            column >> text("pre")
        }

        val len: Int = poems.length
        val file = new File("len.txt")
        val pw = new PrintWriter(file, "UTF-8")
        pw.write(len.toString)
        pw.close()

        poems.zipWithIndex foreach {
            case (poem, index) =>
                val file = new File(s"poem_$index.txt")
                val pw = new PrintWriter(file, "UTF-8")
                pw.write(poem)
                pw.close()
        }
    }


    def prefixWithRoot(to: String): String = {
        root + "/" + to
    }

}