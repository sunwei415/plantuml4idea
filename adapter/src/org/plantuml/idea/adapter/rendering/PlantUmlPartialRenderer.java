package org.plantuml.idea.adapter.rendering;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import net.sourceforge.plantuml.*;
import net.sourceforge.plantuml.core.UmlSource;
import net.sourceforge.plantuml.error.PSystemError;
import net.sourceforge.plantuml.error.PSystemErrorV2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.plantuml.idea.adapter.Format;
import org.plantuml.idea.adapter.Utils;
import org.plantuml.idea.rendering.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.plantuml.idea.adapter.rendering.PlantUmlRendererUtil.*;


public class PlantUmlPartialRenderer extends PlantUmlNormalRenderer {
    private static final Logger logger = Logger.getInstance(PlantUmlPartialRenderer.class);


    @NotNull
    public RenderResult partialRender(RenderRequest renderRequest, @Nullable RenderCacheItem cachedItem, String[] sourceSplit) {
        try {
            FileFormatOption formatOption = new FileFormatOption(Format.from(renderRequest));

            RenderResult renderResult = new RenderResult(RenderingType.PARTIAL, sourceSplit.length);
            for (int page = 0; page < sourceSplit.length; page++) {
                processPage(renderRequest, cachedItem, sourceSplit[page], formatOption, renderResult, page);
            }

            return renderResult;
        } catch (PartialRenderingException e) {
            logger.debug(e);
            return renderError(renderRequest, e);
        }
    }

    @NotNull
    protected RenderResult renderError(RenderRequest renderRequest, PartialRenderingException e) {
        RenderResult renderResult = new RenderResult(RenderingType.PARTIAL, 1);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            LineLocationImpl lineLocation = new LineLocationImpl("", null);
            StringLocated o = new StringLocated("", lineLocation);
            List<StringLocated> data = Collections.singletonList(o);
            UmlSource source = new UmlSource(data, false);

            ErrorUml singleError = new ErrorUml(ErrorUmlType.EXECUTION_ERROR, e.getMessage(), lineLocation);
            PSystemError pSystemError = new PSystemErrorV2(source, data, singleError);
            pSystemError.exportDiagram(os, 0, new FileFormatOption(FileFormat.PNG));
        } catch (IOException e1) {
            logger.warn(e1);
            throw e;
        }
        renderResult.addRenderedImage(new ImageItem(renderRequest.getBaseDir(), renderRequest.getFormat(), renderRequest.getSource(), null, 0, "(Error)", os.toByteArray(), null, RenderingType.PARTIAL, null, null));
        return renderResult;
    }

    public void processPage(RenderRequest renderRequest, @Nullable RenderCacheItem cachedItem, String s, FileFormatOption formatOption, RenderResult renderResult, int page) {
        long partialPageProcessingStart = System.currentTimeMillis();
        String partialSource = "@startuml\n" + s + "\n@enduml";

        boolean obsolete = cachedItem == null
                || renderRequest.requestedRefreshOrIncludesChanged()
                || RenderingType.PARTIAL.renderingTypeChanged(cachedItem)
                || renderRequest.getScaledZoom() != cachedItem.getScaledZoom()
                || !partialSource.equals(cachedItem.getImagesItemPageSource(page));

        boolean pageSelected = renderRequest.getPage() == -1 || renderRequest.getPage() == page;
        boolean shouldRender = pageSelected && (obsolete || !cachedItem.hasImage(page));

        if (shouldRender) {
            Pair<ImageItem, LinkedHashMap<File, Long>> imageItemMapPair = renderImage(renderRequest, page, formatOption, partialSource);
            renderResult.addRenderedImage(imageItemMapPair.first);
            renderResult.addIncludedFiles(imageItemMapPair.second);
        } else if (obsolete) {
            Pair<ImageItem, LinkedHashMap<File, Long>> imageItemMapPair = updateTitle(renderRequest, page, partialSource);
            renderResult.addUpdatedTitle(imageItemMapPair.first);
            renderResult.addIncludedFiles(imageItemMapPair.second);
        } else {
            logger.debug("page ", page, " cached");
            renderResult.addCachedImage(cachedItem.getImageItem(page));
        }
        logger.debug("processing of page ", page, " done in ", System.currentTimeMillis() - partialPageProcessingStart, "ms");
    }

    private Pair<ImageItem, LinkedHashMap<File, Long>> updateTitle(RenderRequest renderRequest, int page, String partialSource) {
        long start = System.currentTimeMillis();
        logger.debug("updating title, page ", page);

        SourceStringReader reader = newSourceStringReader(partialSource, renderRequest.isUseSettings(), renderRequest.getSourceFile());
        String title = getTitle(reader);
        Map<File, Long> includedFiles = Utils.getIncludedFiles(reader);
        
        ImageItem imageItem = new ImageItem(renderRequest.getBaseDir(), renderRequest.getFormat(), renderRequest.getSource(), partialSource, page, RenderResult.TITLE_ONLY, null, null, RenderingType.PARTIAL, title, null);

        logger.debug("updateTitle " + (System.currentTimeMillis() - start));

        return new Pair(imageItem, includedFiles);
    }

    private String getTitle(SourceStringReader reader) {
        DiagramInfo.Titles titles = getTitles(reader.getBlocks());
        if (titles.size() > 1) {
            throw new PartialRenderingException();
        }
        return titles.get(0);
    }


    private Pair<ImageItem, LinkedHashMap<File, Long>> renderImage(RenderRequest renderRequest, int page, FileFormatOption formatOption, String partialSource) {
        logger.debug("rendering partially, page ", page);
        SourceStringReader reader = newSourceStringReader(partialSource, renderRequest.isUseSettings(), renderRequest.getSourceFile());
        DiagramInfo info = zoomDiagram(reader, renderRequest.getScaledZoom());
        Integer totalPages = info.getTotalPages();
        DiagramInfo.Titles titles = info.getTitles();
        LinkedHashMap<File, Long> includedFiles = Utils.getIncludedFiles(reader);
                        
        if (totalPages > 1) {
            throw new PartialRenderingException();
        }
        if (titles.size() > 1) {
            logger.warn("too many titles " + titles + ", partialSource=" + partialSource);
        }
        try {
            ImageItem item = generateImageItem(renderRequest, renderRequest.getSource(), partialSource, reader, formatOption, 0, page, RenderingType.PARTIAL, titles.get(0), info.getFilename());
            ImageItem imageItem = new ImageItem(page, item, renderRequest.getFormat());
            return new Pair<>(imageItem, includedFiles);
        } catch (RenderingCancelledException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


}
