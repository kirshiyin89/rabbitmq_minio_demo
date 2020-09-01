package ebook;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.epub.EpubWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class EBookHandler {

    public static Book openAndSignBook(InputStream origBook, String signedBookName, String readerName) throws IOException {
        EpubReader epubReader = new EpubReader();
        Book book = epubReader.readEpub(origBook);

        // set title
        book.getMetadata().setTitles(new ArrayList<String>() {{
            add("an awesome book");
        }});
        String body = "<div>Especially for " + readerName.toUpperCase() +"</div>";
        Resource res = new Resource(body.getBytes(),"signature.html");
        book.setCoverPage(res);

        // write epub
        EpubWriter epubWriter = new EpubWriter();
        epubWriter.write(book, new FileOutputStream(signedBookName));
        return book;
    }
}
