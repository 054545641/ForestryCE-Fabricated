package forestry.core.platform.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import java.util.Locale;
import java.util.regex.Pattern;

public class StringUtil {

	private static final Pattern camelCaseToUnderscores = Pattern.compile("(.)([A-Z])");

	public static String camelCaseToUnderscores(String uid) {
		return camelCaseToUnderscores.matcher(uid).replaceAll("$1_$2").toLowerCase(Locale.ENGLISH);
	}

	public static String append(String delim, String source, String appendix) {
		if (source.length() <= 0) {
			return appendix;
		}

		if (appendix.length() <= 0) {
			return source;
		}

		return source + delim + appendix;
	}

	public static String floatAsPercent(float val) {
		return (int) (val * 100) + " %";
	}

	public static Component line(int length) {
		StringBuilder line = new StringBuilder();
		for (int i = 0; i < length; i++) {
			line.append('-');
		}

		return Component.literal(line.toString());
	}

	public static int getLineHeight(int maxWidth, FormattedText... strings) {
		Minecraft minecraft = Minecraft.getInstance();
		Font fontRenderer = minecraft.font;

		int lineCount = 0;
		for (FormattedText string : strings) {
			lineCount += fontRenderer.split(string, maxWidth).size();
		}

		return lineCount * fontRenderer.lineHeight;
	}
}
