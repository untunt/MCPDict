package com.osfans.mcpdict;

import static com.osfans.mcpdict.DB.COL_BH;
import static com.osfans.mcpdict.DB.COL_BS;
import static com.osfans.mcpdict.DB.COL_HD;
import static com.osfans.mcpdict.DB.COL_HZ;
import static com.osfans.mcpdict.DB.COL_KX;
import static com.osfans.mcpdict.DB.COL_SW;
import static com.osfans.mcpdict.DB.COMMENT;
import static com.osfans.mcpdict.DB.HD;
import static com.osfans.mcpdict.DB.HZ;
import static com.osfans.mcpdict.DB.KX;
import static com.osfans.mcpdict.DB.SW;
import static com.osfans.mcpdict.DB.UNICODE;
import static com.osfans.mcpdict.DB.VARIANTS;
import static com.osfans.mcpdict.DB.getColor;
import static com.osfans.mcpdict.DB.getColumn;
import static com.osfans.mcpdict.DB.getLabel;
import static com.osfans.mcpdict.DB.getLanguages;
import static com.osfans.mcpdict.DB.getSubColor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.CharacterStyle;
import android.text.style.DrawableMarginSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ParagraphStyle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ResultAdapter extends CursorAdapter {

    private static WeakReference<Context> context;
    private final int layout;
    private final LayoutInflater inflater;
    private final boolean showFavoriteButton;

    public ResultAdapter(Context context, int layout, Cursor cursor, boolean showFavoriteButton) {
        super(context, cursor, FLAG_REGISTER_CONTENT_OBSERVER);
        ResultAdapter.context = new WeakReference<>(context);
        this.layout = layout;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.showFavoriteButton = showFavoriteButton;
    }

    private static Context getContext() {
        return context.get();
    }

    static class ViewHolder implements View.OnClickListener{
        TextView tvHZ, tvUnicode, tvSW, tvKX, tvHD, tvComment, tvVariant;
        Button btnMap, btnFavorite;
        TextView tvReadings;
        boolean isFavorite;
        Set<String> cols;
        String col = "";
        HashMap<String, ParagraphStyle> spans;
        HashMap<String, String> rawTexts;
        HashMap<String, TextView> tvs;

        public ViewHolder(View view) {
            tvHZ = view.findViewById(R.id.text_hz);
            tvHZ.setOnClickListener(this);
            tvUnicode = view.findViewById(R.id.text_unicode);
            tvUnicode.setOnClickListener(this);
            tvSW = view.findViewById(R.id.text_sw);
            tvSW.setText(getLabel(SW));
            tvSW.setOnClickListener(this);
            tvKX = view.findViewById(R.id.text_kx);
            tvKX.setText(getLabel(KX));
            tvKX.setOnClickListener(this);
            tvHD = view.findViewById(R.id.text_hd);
            tvHD.setText(getLabel(HD));
            tvHD.setOnClickListener(this);
            tvComment = view.findViewById(R.id.text_comment);
            tvVariant = view.findViewById(R.id.text_variants);
            tvs = new HashMap<>();
            tvs.put(UNICODE, tvUnicode);
            tvs.put(SW, tvSW);
            tvs.put(KX, tvKX);
            tvs.put(HD, tvHD);
            tvs.put(COMMENT, tvComment);
            tvs.put(VARIANTS, tvVariant);
            btnMap = view.findViewById(R.id.button_map);
            btnMap.setOnClickListener(this);
            btnFavorite = view.findViewById(R.id.button_favorite);
            tvReadings = view.findViewById(R.id.text_readings);
            spans = new HashMap<>();
            rawTexts = new HashMap<>();
            float fontSize = tvReadings.getTextSize();
            TextDrawable.IBuilder builder = TextDrawable.builder()
                    .beginConfig()
                    .withBorder(1)
                    .width((int) (fontSize * 3.1f))  // width in px
                    .height((int) (fontSize * 1.25f)) // height in px
                    .fontSize(fontSize)
                    .endConfig()
                    .roundRect(5);
            for (String lang: getLanguages()) {
                Drawable drawable = builder.build(getLabel(lang), getColor(lang), getSubColor(lang));
                DrawableMarginSpan span = new DrawableMarginSpan(drawable, 10);
                spans.put(lang, span);
            }
        }

        @Override
        public void onClick(View view) {
            Context context = getContext();
            String hz = rawTexts.get(HZ);
            if (view == btnMap) {
                new MyMapView(context, hz).show();
                return;
            } else if (view == tvReadings) {
                BaseActivity activity = (BaseActivity) getContext();
                activity.registerForContextMenu(view);
                activity.openContextMenu(view);
                activity.unregisterForContextMenu(view);
            }
            for (String key: new String[]{UNICODE, SW, KX, HD}) {
                if (view == tvs.get(key)) {
                    TextView showText = new TextView(context);
                    showText.setPadding(24, 24, 24, 24);
                    showText.setTextIsSelectable(true);
                    showText.setMovementMethod(LinkMovementMethod.getInstance());
                    showText.setText(formatPassage(hz, rawTexts.get(key)));
                    new AlertDialog.Builder(context).setView(showText).show();
                    return;
                }
            }
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = inflater.inflate(layout, parent, false);
        ViewHolder holder = new ViewHolder(view);
        view.setTag(holder);
        return view;
    }

    public static CharSequence formatIPA(String lang, String string) {
        CharSequence cs;
        if (TextUtils.isEmpty(string)) return "";
        switch (lang) {
            case DB.SG:
                cs = getRichText(string.replace(",", "  "));
                break;
            case DB.BA:
                cs = baDisplayer.display(string);
                break;
            case DB.GY:
                cs = getRichText(gyDisplayer.display(string));
                break;
            case DB.CMN:
                cs = getRichText(cmnDisplayer.display(string));
                break;
            case DB.HK:
                cs = hkDisplayer.display(string);
                break;
            case DB.TW:
                cs = getRichText(twDisplayer.display(string));
                break;
            case DB.KOR:
                cs = korDisplayer.display(string);
                break;
            case DB.VI:
                cs = viDisplayer.display(string);
                break;
            case DB.JA_GO:
            case DB.JA_KAN:
            case DB.JA_TOU:
            case DB.JA_KWAN:
            case DB.JA_OTHER:
                cs = getRichText(jaDisplayer.display(string));
                break;
            default:
                cs = getRichText(toneDisplayer.display(string, lang));
                break;
        }
        return cs;
    }

    private static CharSequence formatPassage(String hz, String js) {
        String[] fs = (js+"\n").split("\n", 2);
        String s = String.format("<p><big><big><big>%s</big></big></big> %s</p><br><p>%s</p>", hz, fs[0], fs[1].replace("\n", "<br/>"));
        return getRichText(s);
    }

    @Override
    public void bindView(final View view, final Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder)view.getTag();

        String hz, s;
        Set<String> cols = new HashSet<>();
        Orthography.setToneStyle(getStyle(R.string.pref_key_tone_display));
        Orthography.setToneValueStyle(getStyle(R.string.pref_key_tone_value_display));
        hz = cursor.getString(COL_HZ);

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        for (String lang : DB.getVisibleColumns(context)) {
            int i = DB.getColumnIndex(lang);
            s = cursor.getString(i);
            if (TextUtils.isEmpty(s)) continue;
            CharSequence cs = formatIPA(lang, s);
            int n = ssb.length();
            ssb.append(" ", holder.spans.get(lang), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.append(cs);
            ssb.append("\n");
            ssb.setSpan(new MyClickableSpan(lang), n, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            cols.add(lang);
        }
        holder.tvReadings.setText(ssb);
        holder.tvReadings.setMovementMethod(LinkMovementMethod.getInstance());

        for (int i = DB.COL_HZ; i < DB.COL_VA; i++) {
            s = cursor.getString(i);
            if (TextUtils.isEmpty(s)) s = "";
            if (i == COL_BS) s = s.replace("f", "-");
            else if (i == COL_KX) s = s.replaceFirst("^(.*?)(\\d+).(\\d+)", "$1<a href=https://kangxizidian.com/kxhans/" + hz + ">第$2頁第$3字</a>");
            else if (i == COL_HD) s = s.replaceFirst("(\\d+).(\\d+)", "【汉語大字典】<a href=https://www.homeinmists.com/hd/png/$1.png>第$1頁</a>第$2字");
            holder.rawTexts.put(DB.getColumn(i), s);
        }

        // HZ
        TextView tv;
        holder.tvHZ.setText(hz);
        cols.add(HZ);
        // Unicode
        s = Orthography.HZ.toUnicode(hz);
        holder.tvUnicode.setText(s);
        StringBuilder sb = new StringBuilder();
        String unicode = s;
        sb.append(String.format("<p>【統一碼】%s %s</p>", unicode, Orthography.HZ.getUnicodeExt(hz)));
        for (int i = DB.COL_LF; i < DB.COL_VA; i++) {
            if (i == COL_SW) i = COL_BH;
            String lang = getColumn(i);
            s = holder.rawTexts.getOrDefault(lang, "");
            if (TextUtils.isEmpty(s)) continue;
            sb.append(String.format("<p>【%s】%s</p>", lang, s));
        }
        String info = sb.toString().replace(",", ", ");
        holder.rawTexts.put(UNICODE, info);
        // Variants
        s = cursor.getString(cursor.getColumnIndexOrThrow(VARIANTS));
        if (!TextUtils.isEmpty(s) && !s.contentEquals(hz)) {
            s = String.format("(%s)", s);
        } else s = "";
        holder.rawTexts.put(VARIANTS, s);
        holder.tvVariant.setText(s);
        // Favorite comment
        s = cursor.getString(cursor.getColumnIndexOrThrow(COMMENT));
        holder.rawTexts.put(COMMENT, s);
        holder.tvComment.setText(s);

        for (String key: new String[]{SW, KX, HD, VARIANTS, COMMENT}) {
            tv = holder.tvs.get(key);
            s =  holder.rawTexts.get(key);
            tv.setVisibility(TextUtils.isEmpty(s) ? View.GONE: View.VISIBLE);
        }

         // "Favorite" button
        boolean favorite = cursor.getInt(cursor.getColumnIndexOrThrow("is_favorite")) == 1;
        holder.isFavorite = favorite;
        Button button = holder.btnFavorite;
        button.setOnClickListener(v -> {
            if (holder.isFavorite) {
                FavoriteDialogs.view(hz, view);
            } else {
                FavoriteDialogs.add(hz);
            }
        });
        if (showFavoriteButton) {
            button.setBackgroundResource(favorite ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        } else {
            button.setVisibility(View.GONE);
        }

        // Set the view's cols to indicate which readings exist
        holder.cols = cols;
    }

    private static String getHexColor() {
        int color = ContextCompat.getColor(getContext(), R.color.dim);
        return String.format("#%06X", color & 0xFFFFFF);
    }

    private static CharSequence getRichText(String richTextString) {
        String s = richTextString
                .replace("\n", "<br/>")
                .replace("{", "<small><small>")
                .replace("}", "</small></small>")
                .replaceAll("\\*(.+?)\\*", "<b>$1</b>")
                .replaceAll("\\|(.+?)\\|", String.format("<span style='color: %s;'>$1</span>", getHexColor()));
        return HtmlCompat.fromHtml(s, HtmlCompat.FROM_HTML_MODE_COMPACT);
    }

    public static String getRawText(String s) {
        if (TextUtils.isEmpty(s)) return "";
        return s.replaceAll("[|*\\[\\]]", "").replaceAll("\\{.*?\\}", "");
    }

    private static final Displayer gyDisplayer = new Displayer() {
        public String displayOne(String s) {return Orthography.MiddleChinese.display(s, getStyle(R.string.pref_key_mc_display));}
    };

    private static int getStyle(int id) {
        int value = 0;
        if (id == R.string.pref_key_tone_display) value = 1;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        Resources r = getContext().getResources();
        try {
            return Integer.parseInt(Objects.requireNonNull(sp.getString(r.getString(id), String.valueOf(value))));
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return value;
    }

    private static final Displayer cmnDisplayer = new Displayer() {
        public String displayOne(String s) {
            return Orthography.Mandarin.display(s, getStyle(R.string.pref_key_mandarin_display));
        }
    };

    private static final Displayer hkDisplayer = new Displayer() {
        public String displayOne(String s) {
            return Orthography.Cantonese.display(s, getStyle(R.string.pref_key_cantonese_romanization));
        }
    };

    private static final Displayer twDisplayer = new Displayer() {
        public String displayOne(String s) {
            return Orthography.Minnan.display(s, getStyle(R.string.pref_key_minnan_display));
        }
    };

    private static final Displayer baDisplayer = new Displayer() {
        public String displayOne(String s) {
            return s;
        }
    };

    private static final Displayer toneDisplayer = new Displayer() {
        public String displayOne(String s) {
            return Orthography.Tones.display(s, getLang());
        }
    };

    private static final Displayer korDisplayer = new Displayer() {
        public String displayOne(String s) {
            return Orthography.Korean.display(s, getStyle(R.string.pref_key_korean_display));
        }
    };

    private static final Displayer viDisplayer = new Displayer() {
        public String displayOne(String s) {
            return Orthography.Vietnamese.display(s, getStyle(R.string.pref_key_vietnamese_tone_position));
        }
    };

    private static final Displayer jaDisplayer = new Displayer() {
        public String displayOne(String s) {
            return Orthography.Japanese.display(s, getStyle(R.string.pref_key_japanese_display));
        }
    };
}