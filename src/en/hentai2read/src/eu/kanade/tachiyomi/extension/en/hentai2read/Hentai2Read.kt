package eu.kanade.tachiyomi.extension.en.hentai2read

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.FormBody
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class Hentai2Read : ParsedHttpSource() {

    override val name = "Hentai2Read"
    override val baseUrl = "https://hentai2read.com"
    override val lang = "en"
    override val supportsLatest = true

    override val client = network.cloudflareClient

    override fun headersBuilder() = super.headersBuilder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
        .add("Referer", baseUrl)

    // =============================== Popular ================================

    override fun popularMangaRequest(page: Int): Request =
        GET("$baseUrl/hentai-list/all/any/all/most-popular/$page/", headers)

    override fun popularMangaSelector() = ".book-grid-item-container"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        val a = element.selectFirst(".overlay-title a")!!
        setUrlWithoutDomain(a.attr("href"))
        title = a.ownText().trim()
        thumbnail_url = element.selectFirst("picture img")?.absUrl("src")
    }

    override fun popularMangaNextPageSelector() = "ul.pagination li:last-child a"

    // =============================== Latest =================================

    override fun latestUpdatesRequest(page: Int): Request =
        GET("$baseUrl/hentai-list/all/any/all/last-added/$page/", headers)

    override fun latestUpdatesSelector() = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)
    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    // =============================== Search =================================

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val form = FormBody.Builder()
            .add("cmd_wpm_wgt_mng_sch_sbm", "Search")
            .add("txt_wpm_wgt_mng_sch_nme", query)
            .build()
        return POST("$baseUrl/hentai-list/search/", headers, form)
    }

    override fun searchMangaSelector() = popularMangaSelector()
    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)
    override fun searchMangaNextPageSelector(): String? = null

    // ============================= Details ==================================

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        title = document.selectFirst("h3.block-title a")?.ownText()?.trim() ?: ""
        thumbnail_url = document.selectFirst(".img-container picture img")?.absUrl("src")
        author = document.select("a[href*=/author/], a[href*=/artist/]")
            .joinToString { it.text() }
        genre = document.select("a.tagButton[href*=/category/]")
            .joinToString { it.text() }
        status = SManga.COMPLETED
    }

    // ============================= Chapters =================================

    override fun chapterListSelector() = "ul.nav-chapters li"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        val a = element.selectFirst("a.pull-left")!!
        setUrlWithoutDomain(a.attr("href"))
        name = a.ownText().trim()
        date_upload = 0L
    }

    // ============================= Pages ====================================

    override fun pageListParse(document: Document): List<Page> {
        val script = document.selectFirst("script:containsData(gData)")?.data()
            ?: return emptyList()
        val imagesJson = Regex("""'images'\s*:\s*(\[.+?\])""", RegexOption.DOT_MATCHES_ALL)
            .find(script)?.groupValues?.get(1) ?: return emptyList()
        return Regex(""""([^"]+)"""").findAll(imagesJson)
            .mapIndexed { i, m ->
                val path = m.groupValues[1].replace("\\/", "/")
                Page(i, imageUrl = "https://static.hentaicdn.com/hentai$path")
            }.toList()
    }

    override fun imageUrlParse(document: Document) = ""
}
