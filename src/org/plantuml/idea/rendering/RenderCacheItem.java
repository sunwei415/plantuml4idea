package org.plantuml.idea.rendering;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

public class RenderCacheItem {
    private static final Logger LOG = Logger.getInstance(RenderCacheItem.class);

    private final RenderRequest renderRequest;
    private final RenderResult renderResult;
    private final String[] titles;

    private ImageItem[] imageItems;
    private Integer version;
    private int requestedPage;

    public RenderCacheItem(@NotNull RenderRequest renderRequest, RenderResult renderResult, int requestedPage, int version) {
        this.renderRequest = renderRequest;
        this.renderResult = renderResult;

        imageItems = renderResult.getImageItemsAsArray();
        this.titles = new String[imageItems.length];
        for (int i = 0; i < imageItems.length; i++) {
            ImageItem imageItem = imageItems[i];
            titles[i] = imageItem != null ? imageItem.getTitle() : null;
        }
        this.requestedPage = requestedPage;
        this.version = version;
    }


    public RenderRequest getRenderRequest() {
        return renderRequest;
    }

    public String[] getTitles() {
        return titles;
    }

    public boolean imageMissingOrSourceOrZoomChanged(String source, int page, int scaledZoom) {
        if (imageMissing(page)) {
            LOG.debug("image missing");
            return true;
        }
        if (scaledZoom != renderRequest.getScaledZoom()) {
            LOG.debug("zoom changed");
            return true;
        }
        if (!renderRequest.getSource().equals(source)) {
            LOG.debug("source changed");
            return true;
        }
        LOG.debug("imageMissingOrSourceOrZoomChanged = false");
        return false;
    }

    private boolean imageMissing(int page) {
        if (page == -1) {
            for (int i = 0; i < imageItems.length; i++) {
                if (!hasImage(i)) {
                    return true;
                }
            }
        } else {
            if (!hasImage(page)) {
                return true;
            }
        }
        return false;
    }


    public String getSourceFilePath() {
        return renderRequest.getSourceFilePath();
    }

    public String getSource() {
        return renderRequest.getSource();
    }

    public File getBaseDir() {
        return renderRequest.getBaseDir();
    }

    public RenderResult getRenderResult() {
        return renderResult;
    }

    public ImageItem[] getImageItems() {
        return imageItems;
    }

    public boolean includedFilesChanged(FileDocumentManager fileDocumentManager, VirtualFileManager virtualFileManager) {
        boolean result = false;
        Map<File, Long> includedFiles = renderResult.getIncludedFiles();
        if (includedFiles != null) {
            for (Map.Entry<File, Long> fileLongEntry : renderResult.getIncludedFiles().entrySet()) {
                File file = fileLongEntry.getKey();
                Long timestamp = fileLongEntry.getValue();
                VirtualFile virtualFile = virtualFileManager.findFileByUrl("file://" + file.getAbsolutePath());
                Document document = null;
                if (virtualFile != null) {
                    document = fileDocumentManager.getDocument(virtualFile);
                }
                if (timestamp != null && isChanged(fileDocumentManager, file, timestamp, document)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    private static boolean isChanged(FileDocumentManager fileDocumentManager, File file, Long timestamp, Document document) {
        return timestamp < file.lastModified() || (document != null && fileDocumentManager.isDocumentUnsaved(document));
    }

    public boolean isIncludedFile(@Nullable VirtualFile file) {
        if (file == null) {
            return false;
        }
        File key = new File(file.getPath());
        Long aLong = renderResult.getIncludedFiles().get(key);
        return aLong != null;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Integer getVersion() {
        return version;
    }

    public int getScaledZoom() {
        return renderRequest.getScaledZoom();
    }

    public int getRequestedPage() {
        return requestedPage;
    }

    public void setRequestedPage(int requestedPage) {
        this.requestedPage = requestedPage;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("version", version)
                .append("renderRequest", renderRequest)
                .append("page", requestedPage)
                .append("imageResult", renderResult)
                .append("imagesWithData", Arrays.toString(imageItems))
                .toString();
    }

    public String getImagesItemPageSource(int page) {
        if (imageItems.length > page) {
            ImageItem imageItem = imageItems[page];
            if (imageItem != null) {
                return imageItem.getPageSource();
            }
        }
        return null;
    }

    @Nullable
    public ImageItem getImageItem(int page) {
        return imageItems.length > page ? imageItems[page] : null;
    }

    public boolean hasImage(int i) {
        ImageItem imageItem = getImageItem(i);
        if (imageItem != null) {
            return imageItem.hasImage();
        }
        return false;
    }

    public boolean titleChaged(String s, int i) {
        ImageItem imageItem = getImageItem(i);
        if (imageItem != null) {
            String title = imageItem.getTitle();
            if (title == null && s == null) {
                return false;
            }
            return !StringUtils.equals(s, title);
        }
        return true;
    }
}
