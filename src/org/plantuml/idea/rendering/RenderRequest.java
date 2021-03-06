package org.plantuml.idea.rendering;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.plantuml.idea.plantuml.PlantUml;
import org.plantuml.idea.util.UIUtils;

import java.io.File;

public class RenderRequest {
    private final String sourceFilePath;
    @NotNull
    private final String source;
    @NotNull
    private final PlantUml.ImageFormat format;
    private final int page;
    private final int scaledZoom;
    private final Integer version;
    private boolean renderUrlLinks;
    private RenderCommand.Reason reason;
    protected boolean useSettings = true;

    public RenderRequest(String sourceFilePath,
                         @NotNull String source,
                         @NotNull PlantUml.ImageFormat format,
                         int page,
                         int scaledZoom,
                         Integer version,
                         boolean renderUrlLinks,
                         RenderCommand.Reason reason) {
        this.sourceFilePath = sourceFilePath;
        this.source = source;
        this.format = format;
        this.page = page;
        this.scaledZoom = scaledZoom;
        this.version = version;
        this.renderUrlLinks = renderUrlLinks;
        this.reason = reason;
    }

    public RenderRequest(@NotNull RenderRequest renderRequest,
                         @NotNull PlantUml.ImageFormat format) {
        this.sourceFilePath = renderRequest.sourceFilePath;
        this.source = renderRequest.source;
        this.format = format;
        this.page = renderRequest.page;
        this.scaledZoom = renderRequest.scaledZoom;
        this.useSettings = renderRequest.useSettings;
        this.version = null;
    }


    @NotNull
    public String getSource() {
        return source;
    }

    public String getSourceFilePath() {
        return sourceFilePath;
    }

    public File getSourceFile() {
        return new File(sourceFilePath);
    }

    @NotNull
    public PlantUml.ImageFormat getFormat() {
        return format;
    }

    public int getPage() {
        return page;
    }

    public int getScaledZoom() {
        return scaledZoom;
    }

    public Integer getVersion() {
        return version;
    }

    public boolean isRenderUrlLinks() {
        return renderUrlLinks;
    }

    public RenderCommand.Reason getReason() {
        return reason;
    }

    public boolean isUseSettings() {
        return useSettings;
    }

    public void setUseSettings(boolean useSettings) {
        this.useSettings = useSettings;
    }

    public boolean requestedRefreshOrIncludesChanged() {
        return reason == RenderCommand.Reason.REFRESH || reason == RenderCommand.Reason.INCLUDES;
    }

    public File getBaseDir() {
        return UIUtils.getParent(new File(sourceFilePath));
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("sourceFilePath", sourceFilePath)
                .append("format", format)
                .append("page", page)
                .append("scaledZoom", scaledZoom)
                .append("renderUrlLinks", renderUrlLinks)
                .append("reason", reason)
                .append("version", version)
                .append("useSettings", useSettings)
                .toString();
    }


}
