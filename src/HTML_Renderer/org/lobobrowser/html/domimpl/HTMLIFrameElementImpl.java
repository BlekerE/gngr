package org.lobobrowser.html.domimpl;

import java.net.URL;

import org.lobobrowser.html.BrowserFrame;
import org.lobobrowser.html.js.Executor;
import org.lobobrowser.html.js.Window;
import org.lobobrowser.html.js.Window.JSRunnableTask;
import org.lobobrowser.html.style.IFrameRenderState;
import org.lobobrowser.html.style.RenderState;
import org.lobobrowser.js.HideFromJS;
import org.lobobrowser.ua.UserAgentContext.Request;
import org.lobobrowser.ua.UserAgentContext.RequestKind;
import org.mozilla.javascript.Function;
import org.w3c.dom.Document;
import org.w3c.dom.html.HTMLIFrameElement;

public class HTMLIFrameElementImpl extends HTMLAbstractUIElement implements HTMLIFrameElement, FrameNode {
  private volatile BrowserFrame browserFrame;

  public HTMLIFrameElementImpl(final String name) {
    super(name);
  }

  @HideFromJS
  public void setBrowserFrame(final BrowserFrame frame) {
    this.browserFrame = frame;
    createJob();
  }

  private boolean jobCreated = false;

  private void createJob() {
    synchronized (this) {
      final String src = this.getAttribute("src");
      if (src != null) {
        if (!jobCreated) {
          ((HTMLDocumentImpl) document).addJob(() -> loadURLIntoFrame(src));
          jobCreated = true;
        } else {
          ((HTMLDocumentImpl) document).addJob(() -> loadURLIntoFrame(src), 0);
        }
      }
    }
  }

  private void markJobDone() {
    synchronized (this) {
      ((HTMLDocumentImpl) document).markJobsFinished(1);
      jobCreated = false;
    }
  }

  public BrowserFrame getBrowserFrame() {
    return this.browserFrame;
  }

  public String getAlign() {
    return this.getAttribute("align");
  }

  public Document getContentDocument() {
    // TODO: Domain-based security
    final BrowserFrame frame = this.browserFrame;
    if (frame == null) {
      // Not loaded yet
      return null;
    }
    return frame.getContentDocument();
  }

  public Window getContentWindow() {
    final BrowserFrame frame = this.browserFrame;
    if (frame == null) {
      // Not loaded yet
      return null;
    }
    return Window.getWindow(frame.getHtmlRendererContext());
  }

  public String getFrameBorder() {
    return this.getAttribute("frameborder");
  }

  public String getHeight() {
    return this.getAttribute("height");
  }

  public String getLongDesc() {
    return this.getAttribute("longdesc");
  }

  public String getMarginHeight() {
    return this.getAttribute("marginheight");
  }

  public String getMarginWidth() {
    return this.getAttribute("marginwidth");
  }

  public String getName() {
    return this.getAttribute("name");
  }

  public String getScrolling() {
    return this.getAttribute("scrolling");
  }

  public String getSrc() {
    return this.getAttribute("src");
  }

  public String getWidth() {
    return this.getAttribute("width");
  }

  public void setAlign(final String align) {
    this.setAttribute("align", align);
  }

  public void setFrameBorder(final String frameBorder) {
    this.setAttribute("frameborder", frameBorder);
  }

  public void setHeight(final String height) {
    this.setAttribute("height", height);
  }

  public void setLongDesc(final String longDesc) {
    this.setAttribute("longdesc", longDesc);
  }

  public void setMarginHeight(final String marginHeight) {
    this.setAttribute("marginHeight", marginHeight);
  }

  public void setMarginWidth(final String marginWidth) {
    this.setAttribute("marginWidth", marginWidth);
  }

  public void setName(final String name) {
    this.setAttribute("name", name);
  }

  public void setScrolling(final String scrolling) {
    this.setAttribute("scrolling", scrolling);
  }

  public void setSrc(final String src) {
    this.setAttribute("src", src);
  }

  public void setWidth(final String width) {
    this.setAttribute("width", width);
  }

  @Override
  protected void assignAttributeField(final String normalName, final String value) {
    super.assignAttributeField(normalName, value);

    if ("src".equals(normalName)) {
      createJob();
    }
  }

  private Function onload;

  public Function getOnload() {
    return this.getEventFunction(this.onload, "onload");
  }

  public void setOnload(final Function onload) {
    this.onload = onload;
  }

  private void loadURLIntoFrame(final String value) {
    final BrowserFrame frame = this.browserFrame;
    if (frame != null) {
      try {
        final URL fullURL = this.getFullURL(value);
        if (getUserAgentContext().isRequestPermitted(new Request(fullURL, RequestKind.Frame))) {
          getContentWindow().setJobFinishedHandler(new Runnable() {
            public void run() {
              System.out.println("Iframes window's job over!");
              if (onload != null) {
                // TODO: onload event object?
                final Window window = ((HTMLDocumentImpl) document).getWindow();
                window.addJSTask(new JSRunnableTask(0, "IFrame onload handler", () -> {
                  Executor.executeFunction(HTMLIFrameElementImpl.this, onload, null, window.getContextFactory());
                }));
              }
              markJobDone();
            }
          });
          frame.loadURL(fullURL);
        }
      } catch (final java.net.MalformedURLException mfu) {
        this.warn("loadURLIntoFrame(): Unable to navigate to src.", mfu);
      } finally {
        /* TODO: Implement an onload handler
        // Copied from image element
        final Function onload = this.getOnload();
        System.out.println("onload: " + onload);
        if (onload != null) {
          // TODO: onload event object?
          Executor.executeFunction(HTMLIFrameElementImpl.this, onload, null);
        }*/
      }
    }
  }

  @Override
  protected RenderState createRenderState(final RenderState prevRenderState) {
    return new IFrameRenderState(prevRenderState, this);
  }
}
