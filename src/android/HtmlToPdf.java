package xyz.xyzmax.cordova.HtmlToPdf;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Bitmap;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontProvider;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Entities;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

@TargetApi(19)
public class HtmlToPdf extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        try {
            if (action.equals("create")) {
                final String css = InputStreamToString(getClass().getResourceAsStream("/assets/www/css/pdf.css"));

                final Html2pdf self = this;
                final String content = args.optString(0, "<html></html>");
                cordova.getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        self.init(content, css);
                    }
                });
                PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);

                return true;
            }
            return false;
        } catch (Exception e) {
            callbackContext.error(e.getMessage());
            return false;
        }
    }

    private String InputStreamToString(InputStream is) throws IOException {
        ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
        int ch;
        while ((ch = is.read()) != -1) {
            bytestream.write(ch);
        }
        byte imgdata[] = bytestream.toByteArray();
        bytestream.close();
        return new String(imgdata);
    }

    private void init(String content, String css) {
        Activity ctx = cordova.getActivity();
        WebView mWebView = new WebView(ctx);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(new InJavaScriptLocalObj(css), "local_obj");
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.requestFocus();
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        String baseURL = webView.getUrl();
        baseURL = baseURL.substring(0, baseURL.lastIndexOf('/') + 1);
        mWebView.loadDataWithBaseURL(baseURL, content, "text/html", "utf-8", null);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.loadUrl("javascript:window.local_obj.showSource('<head>'+"
                        + "document.getElementsByTagName('html')[0].innerHTML+'</head>');");
            }

            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        });
    }

}

final class InJavaScriptLocalObj {

    private static final String fontPath = "/xyz/xyzmax/cordov/simsun.ttf";
    private String CssStyle;

    public InJavaScriptLocalObj(String css) {
        this.CssStyle = css;
    }


    @JavascriptInterface
    public void showSource(String html) throws FileNotFoundException {
        try {

            System.out.print(html);

            BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            final Font font = new Font(bf, 13);

            StringBuilder sb = new StringBuilder(this.CssStyle);
            sb = sb.insert(0, "</title><style>");
            this.CssStyle = sb.append("</style></head>").toString();

            OutputStream file = new FileOutputStream("/data/data/cn.gov.gdepb.ydzf/test.html");
            String result = tidyHtml(html);
            String pattern = "<script(?:\\s+[^>]*)?>(.*?)<\\/script\\s*>";
            result = Pattern.compile(pattern).matcher(result).replaceAll("");
            pattern = "<link[^>]*.*/>";
            result = Pattern.compile(pattern).matcher(result).replaceAll("");
            pattern = "</title>\\s*</head> ";
            result = Pattern.compile(pattern).matcher(result).replaceAll(this.CssStyle);
            file.write(result.getBytes());
            file.close();


            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("/data/data/cn.gov.gdepb.ydzf/test.pdf"));
            document.open();
            XMLWorkerHelper.getInstance().parseXHtml(writer, document,
                    new FileInputStream("/data/data/cn.gov.gdepb.ydzf/test.html"), null, new FontProvider() {
                        @Override
                        public boolean isRegistered(String s) {
                            return false;
                        }

                        @Override
                        public Font getFont(String s, String s1, boolean b, float v, int i, BaseColor baseColor) {
                            return font;
                        }
                    });
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String tidyHtml(String html) {
        org.jsoup.nodes.Document doc = Jsoup.parse(html);
        doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
        doc.outputSettings().prettyPrint(true);
        doc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
        return doc.toString();
    }
}
