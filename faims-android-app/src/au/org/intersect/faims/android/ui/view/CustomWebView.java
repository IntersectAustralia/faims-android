package au.org.intersect.faims.android.ui.view;

import java.util.Stack;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.managers.CSSManager;
import au.org.intersect.faims.android.util.FileUtil;

import com.google.inject.Inject;

public class CustomWebView extends WebView implements IView {
	
	private static class WebPage {
		String data;
		String mimeType;
		String encoding;
		String failUrl;
		
		public WebPage(String data, String mimeType, String encoding, String failUrl) {
			this.data = data;
			this.mimeType = mimeType;
			this.encoding = encoding;
			this.failUrl = failUrl;
		}
	}
	
	@Inject
	CSSManager cssManager;

	private Stack<WebPage> webPageStack;
	
	private String baseUrl;

	private String ref;
	private boolean dynamic;

	public CustomWebView(final Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	public CustomWebView(final Context context, String ref, boolean dynamic) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		
		this.ref = ref;
		this.dynamic = dynamic;
		webPageStack = new Stack<WebPage>();
		
		this.setWebViewClient(new WebViewClient() {
			@Override
		    public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (URLUtil.isFileUrl(url)) {
					String html = FileUtil.readFileIntoString(url.replace("file://", ""));
					loadDataWithBaseURL(html, "text/html", "utf-8", "");
					return true;
				} else {
					Uri uri = Uri.parse(url);
			        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			        context.startActivity(intent);
			        return true;
				}
		    }
		});
		cssManager.addCSS(this, "web-view");
	}
	
	public boolean isDynamic() {
		return dynamic;
	}
	
	public String getRef() {
		return ref;
	}

	public void loadDataWithBaseURL(String data, String mimeType, String encoding, String failUrl) {
		WebPage webPage = new WebPage(data, mimeType, encoding, failUrl);
		webPageStack.push(webPage);
		loadWebPage();
	}
	
	private void loadWebPage() {
		WebPage webPage = webPageStack.peek();
		super.loadDataWithBaseURL(baseUrl, webPage.data, webPage.mimeType, webPage.encoding, webPage.failUrl);
	}

	@Override
	public boolean canGoBack() {
		return webPageStack.size() > 1;
	}

	@Override
	public void goBack() {
		if (canGoBack()) {
			webPageStack.pop();
			loadWebPage();
		}
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	@Override
	public String getClickCallback() {
		return null;
	}

	@Override
	public void setClickCallback(String code) {
		
	}

	@Override
	public String getSelectCallback() {
		return null;
	}

	@Override
	public void setSelectCallback(String code) {
		
	}

	@Override
	public String getFocusCallback() {
		return null;
	}

	@Override
	public String getBlurCallback() {
		return null;
	}

	@Override
	public void setFocusBlurCallbacks(String focusCode, String blurCode) {
	}
}
