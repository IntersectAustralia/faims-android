package au.org.intersect.faims.android.ui.view;

import java.util.Stack;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import au.org.intersect.faims.android.util.FileUtil;

public class CustomWebView extends WebView {
	
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

	private Stack<WebPage> webPageStack;
	
	private String baseUrl;

	public CustomWebView(final Context context) {
		super(context);
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
}
