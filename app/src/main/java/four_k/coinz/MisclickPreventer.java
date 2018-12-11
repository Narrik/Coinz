package four_k.coinz;

import android.os.SystemClock;

public class MisclickPreventer {
    private static long lastClickTime = 0;

    public static boolean cantClickAgain() {
        boolean cantClick = SystemClock.elapsedRealtime() - lastClickTime < 1500;
        lastClickTime = SystemClock.elapsedRealtime();
        return cantClick;
    }
}
