package com.timecat.module.git.sgit.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.timecat.component.commonsdk.utils.override.LogUtil;
import com.timecat.element.alert.ToastUtil;
import com.timecat.module.git.R;
import com.timecat.module.git.sgit.activities.SheimiFragmentActivity;
import com.timecat.module.git.sgit.activities.ViewFileActivity;
import com.timecat.module.git.utils.CodeGuesser;
import com.timecat.module.git.utils.Profile;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by phcoder on 09.12.15.
 */
public class ViewFileFragment extends BaseFragment {
    private WebView mFileContent;
    private static final String JS_INF = "CodeLoader";
    private ProgressBar mLoading;
    private File mFile;
    private short mActivityMode = ViewFileActivity.TAG_MODE_NORMAL;
    private boolean mEditMode = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.git_fragment_view_file, container, false);

        mFileContent = (WebView) v.findViewById(R.id.fileContent);
        mLoading = (ProgressBar) v.findViewById(R.id.loading);

        String fileName = null;
        if (savedInstanceState != null) {
            fileName = savedInstanceState.getString(ViewFileActivity.TAG_FILE_NAME);
            mActivityMode = savedInstanceState.getShort(ViewFileActivity.TAG_MODE, ViewFileActivity.TAG_MODE_NORMAL);
        }
        if (fileName == null) {
            fileName = getArguments().getString(ViewFileActivity.TAG_FILE_NAME);
            mActivityMode = getArguments().getShort(ViewFileActivity.TAG_MODE, ViewFileActivity.TAG_MODE_NORMAL);
        }

        mFile = new File(fileName);
        mFileContent.addJavascriptInterface(new CodeLoader(), JS_INF);
        WebSettings webSettings = mFileContent.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mFileContent.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onConsoleMessage(String message, int lineNumber,
                    String sourceID) {
                Log.d("MyApplication", message + " -- From line " + lineNumber
                        + " of " + sourceID);
            }

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });
        mFileContent.setBackgroundColor(Color.TRANSPARENT);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFileContent();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mEditMode) {
            mFileContent.loadUrl(CodeGuesser.wrapUrlScript("save();"));
        }
    }

    private void loadFileContent() {
        mFileContent.loadUrl("file:///android_asset/editor.html");
        mFileContent.setFocusable(mEditMode);
    }

    public boolean getEditMode() {
        return mEditMode;
    }

    public File getFile() {
        return mFile;
    }

    public void setEditMode(boolean mode) {
        mEditMode = mode;
        mFileContent.setFocusable(mEditMode);
        mFileContent.setFocusableInTouchMode(mEditMode);
        if (mEditMode) {
            mFileContent.loadUrl(CodeGuesser.wrapUrlScript("setEditable();"));
            ToastUtil.ok_long(R.string.git_msg_now_you_can_edit);
        } else {
            mFileContent.loadUrl(CodeGuesser.wrapUrlScript("save();"));
        }
    }

    public void copyAll() {
        mFileContent.loadUrl(CodeGuesser.wrapUrlScript("copy_all();"));
    }

    public void setLanguage(String lang) {
        String js = String.format("setLang('%s')", lang);
        mFileContent.loadUrl(CodeGuesser.wrapUrlScript(js));
    }

    private class CodeLoader {

        private String mCode;

        @JavascriptInterface
        public String getCode() {
            return mCode;
        }

        @JavascriptInterface
        public void copy_all(final String content) {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("git", content);
            clipboard.setPrimaryClip(clip);
        }

        @JavascriptInterface
        public void save(final String content) {
            if (mActivityMode == ViewFileActivity.TAG_MODE_SSH_KEY) {
                return;
            }
            if (content == null) {
                ToastUtil.e_long(R.string.git_alert_save_failed);
                return;
            }
            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        writeStringToFile(content, mFile);
                    } catch (IOException e) {
                        LogUtil.e(e);
                        showUserError(R.string.git_alert_save_failed);
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadFileContent();
                            ToastUtil.ok_long(R.string.git_success_save);
                        }
                    });
                }
            });
            thread.start();
        }

        @JavascriptInterface()
        public void loadCode() {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mCode = readStringFromFile(mFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                        showUserError(R.string.git_error_can_not_open_file);
                    }
                    display();
                }
            });
            thread.start();
        }

        @JavascriptInterface
        public String getTheme() {
            return Profile.getCodeMirrorTheme(getActivity());
        }

        private void display() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String lang;
                    if (mActivityMode == ViewFileActivity.TAG_MODE_SSH_KEY) {
                        lang = null;
                    } else {
                        lang = CodeGuesser.guessCodeType(mFile.getName());
                    }
                    String js = String.format("setLang('%s')", lang);
                    mFileContent.loadUrl(CodeGuesser.wrapUrlScript(js));
                    mLoading.setVisibility(View.INVISIBLE);
                    mFileContent.loadUrl(CodeGuesser
                            .wrapUrlScript("display();"));
                    if (mEditMode) {
                        mFileContent.loadUrl(CodeGuesser
                                .wrapUrlScript("setEditable();"));
                    }
                }
            });
        }
    }

    public static void writeStringToFile(String str, File file) throws FileNotFoundException {
        try (PrintWriter out = new PrintWriter(file)) {
            out.write(str);
        }
    }

    public static String readStringFromFile(File file) throws IOException {
        StringBuffer fileData = new StringBuffer();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        char[] buf = new char[1024];
        int numRead;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mEditMode = savedInstanceState.getBoolean("EditMode", false);
        }
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("EditMode", mEditMode);
    }

    @Override
    public void reset() {
    }

    @Override
    public SheimiFragmentActivity.OnBackClickListener getOnBackClickListener() {
        return new SheimiFragmentActivity.OnBackClickListener() {
            @Override
            public boolean onClick() {
                return false;
            }
        };
    }

    private void showUserError(final int errorMessageId) {
        SheimiFragmentActivity activity = (SheimiFragmentActivity) getActivity();
        if (activity == null) {return;}
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.showMessageDialog(R.string.git_dialog_error_title, getString(errorMessageId));
            }
        });
    }
}
