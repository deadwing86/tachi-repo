package eu.kanade.tachiyomi.extension.en.hentai2read

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

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
        GET("$baseUrl/browse/type/gallery/page/$page/", headers)

    override fun popularMangaSelector() = "ul.list-item-content li"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        val a = element.selectFirst("a[href]")!!
        setUrlWithoutDomain(a.attr("href"))
        title = element.selectFirst(".manga-title, h3, .title")?.text()
            ?: a.attr("title")
        thumbnail_url = element.selectFirst("img")?.absUrl("src")
    }

    override fun popularMangaNextPageSelector() = "li.next a, .pagination li:last-child:not(.disabled) a"

    // =============================== Latest =================================

    override fun latestUpdatesRequest(page: Int): Request =
        GET("$baseUrl/browse/type/gallery/order/updated/page/$page/", headers)

    override fun latestUpdatesSelector() = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)
    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    // =============================== Search =================================

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/search/".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("page", page.toString())
            .build()
        return GET(url, headers)
    }

    override fun searchMangaSelector() = popularMangaSelector()
    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)
    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    // ============================= Details ==================================

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        title = document.selectFirst("h1, .manga-title")?.text() ?: ""
        thumbnail_url = document.selectFirst(".cover img, .manga-cover img")?.absUrl("src")
        author = document.select(".tag-container:contains(Artists) a, .info a[href*=artist]")
            .joinToString { it.text() }
        genre = document.select(".tag-container a, .tags a")
            .joinToString { it.text() }
        description = document.selectFirst(".description, .manga-desc")?.text()
        status = SManga.COMPLETED
    }

    // ============================= Chapters =================================

    override fun chapterListSelector() = "ul.chapters li, ol li"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        val a = element.selectFirst("a[href]")!!
        setUrlWithoutDomain(a.attr("href"))
        name = a.text().ifBlank { element.text() }
        date_upload = parseDate(element.selectFirst(".date, time")?.text())
    }

    private fun parseDate(text: String?): Long {
        if (text.isNullOrBlank()) return 0L
        return runCatching {
            SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).parse(text)?.time ?: 0L
        }.getOrDefault(0L)
    }

    // ============================= Pages ====================================

    override fun pageListParse(document: Document): List<Page> {
        return document.select("img.img-responsive[src*=static], #all img[src]")
            .mapIndexed { i, img -> Page(i, imageUrl = img.absUrl("src")) }
            .ifEmpty {
                val script = document.selectFirst("script:containsData(imglist)")?.data() ?: ""
                val regex = Regex("""["'](https?://[^"']+\.(?:jpg|png|webp|gif))["']""")
                regex.findAll(script).mapIndexed { i, m -> Page(i, imageUrl = m.groupValues[1]) }.toList()
            }
    }

    override fun imageUrlParse(document: Document) = ""
}