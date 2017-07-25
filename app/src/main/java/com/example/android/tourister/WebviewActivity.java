package com.example.android.tourister;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class WebviewActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar webViewProgressBar;
    private String webURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webView = (WebView) findViewById(R.id.webView);
        webViewProgressBar = (ProgressBar) findViewById(R.id.webViewProgressBar);

        Intent intent = getIntent();
        webURL = intent.getStringExtra("webURL");

        loadWebPage();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.webview_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            loadWebPage();
        }
        else if (item.getItemId() == R.id.share) {
            String mimeType = "text/plain";
            String title = "Share URL";

            ShareCompat.IntentBuilder.from(this)
                    .setChooserTitle(title)
                    .setType(mimeType)
                    .setText(webURL)
                    .startChooser();
        }
        return true;
    }

    private void loadWebPage() {
        webView.setInitialScale(1);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);

        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                webViewProgressBar.setProgress(progress);
                if (progress == 100) {
                    webViewProgressBar.setVisibility(View.GONE);
                }
                else {
                    webViewProgressBar.setVisibility(View.VISIBLE);
                }
            }
        });
        webView.setWebViewClient(new CustomWebViewClient());
        webView.loadUrl(webURL);
    }

    private class CustomWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }
}
