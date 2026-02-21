package com.wedding.backend.service;

import com.linecorp.bot.messaging.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.net.URIBuilder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HighlightService {

    private static final List<String> photoIds = new ArrayList<>(
            Arrays.asList("1QC6D7DvOm-256AfumHvJ1JGxOo9ogPg5", "1pArhS91lYH08aLd8DTq45XMI_vOGZeOJ", "1DqfvCEz-pxBsjPrFJPTG6OHhgZpS_Oyd",
                    "1juALmZmhN3lz5ncbg3F7z6Chv-y4KMhs", "15KnhD1m-0xGoNJzXMioUtSXEiHtu86mj"));

    private static final String GOOGLE_DRIVE_PHOTO_VIEW_URL = "https://drive.google.com/uc?export=view&id=%s";
    private static final String ENDING_PAGE_PHOTO = "https://drive.google.com/uc?export=view&id=1BcA9GltTXNOMkBs9-diu_Yojfow61Umw";
    private static final String GOOGLE_ALBUM_URL = "https://drive.google.com/drive/folders/1XDpK-pPbwcX3FK1aOURnKWHPfuN47aTO";

    public List<String> getFeaturedPhotoFromCloudAlbum() {
        try {
            return photoIds.stream()
                    .map(id -> String.format(GOOGLE_DRIVE_PHOTO_VIEW_URL, id))
                    .toList();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to load featured photos from cloud album.");
        }
    }

    public FlexCarousel buildCarouselImage(List<String> images) {
        List<FlexBubble> imageBubbles = new ArrayList<>(
                images.stream().limit(10).map(this::generateImageBubble).toList()
        );
        // 照片還沒整理好
        imageBubbles.add(generateEndingPage());
        return new FlexCarousel.Builder(imageBubbles).build();
    }

    private FlexBubble generateImageBubble(String url) {
        FlexImage heroImage = new FlexImage.Builder(generateURI(url))
                .size("full")
                .aspectRatio("3:4")
                .aspectMode(FlexImage.AspectMode.COVER)
                .build();

        return new FlexBubble.Builder()
                .hero(heroImage)
                .build();

    }

    private URI generateURI(String url) {
        try {
            return new URIBuilder(url).build();
        } catch (Exception e) {
            return null;
        }
    }

    private FlexBubble generateEndingPage() {
        // 圖片：用 Image 當 body 的第一層，避免 hero 留白問題
        FlexImage image = new FlexImage.Builder(generateURI(ENDING_PAGE_PHOTO))
                .size("full")
                .aspectMode(FlexImage.AspectMode.COVER)
                .aspectRatio("3:4")
                .build();

        // 2. 文字組件 (FlexText) 帶有 Action
        FlexText text = new FlexText.Builder()
                .text("點我點我 ଘ(੭ˊ꒳ ˋ)੭✧˙˚")
                .color("#FFFFFF")
                .weight(FlexText.Weight.BOLD)
                .align(FlexText.Align.CENTER)
                .action(new URIAction("open", URI.create(GOOGLE_ALBUM_URL), null))
                .build();

        // overlay 按鈕（用 absolute 蓋在圖片上，不會留白）
        FlexBox overlayButton = new FlexBox.Builder(FlexBox.Layout.VERTICAL, List.of(text))
                .position(FlexBox.Position.ABSOLUTE)
                .offsetBottom("16px")
                .offsetStart("30px")
                .offsetEnd("30px")
                .backgroundColor("#66666699")
                .cornerRadius("24px")
                .paddingAll("10px")
                .build();

        // 圖片 + overlay 放在同一個 container
        FlexBox body = new FlexBox.Builder(FlexBox.Layout.VERTICAL, List.of(image, overlayButton))
                .paddingAll("0px")
                .build();

        return new FlexBubble.Builder()
                .body(body)
                .build();
    }
}
