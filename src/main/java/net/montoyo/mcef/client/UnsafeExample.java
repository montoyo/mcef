package net.montoyo.mcef.client;

import sun.misc.Unsafe;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.security.Key;

public class UnsafeExample {
    private static final Unsafe theUnsafe;

    private static final Field keyCode;
    private static final Field keyChar;
    private static final Field keyLocation;
    private static final Field id;
    private static final Field when;
    private static final Field modifs;

    private static final long offsetCode;
    private static final long offsetChar;
    private static final long offsetLocation;
    private static final long offsetId;
    private static final long offsetWhen;
    private static final long offsetModifs;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            theUnsafe = (Unsafe) f.get(null);
            
            keyCode = KeyEvent.class.getDeclaredField("keyCode");
            keyChar = KeyEvent.class.getDeclaredField("keyChar");
            keyLocation = KeyEvent.class.getDeclaredField("keyLocation");
            id = AWTEvent.class.getDeclaredField("id");
            when = InputEvent.class.getDeclaredField("when");
            modifs = InputEvent.class.getDeclaredField("modifiers");
            
            offsetCode = theUnsafe.objectFieldOffset(keyCode);
            offsetChar = theUnsafe.objectFieldOffset(keyChar);
            offsetLocation = theUnsafe.objectFieldOffset(keyLocation);
            offsetId = theUnsafe.objectFieldOffset(id);
            offsetWhen = theUnsafe.objectFieldOffset(when);
            offsetModifs = theUnsafe.objectFieldOffset(modifs);
        } catch (Throwable e) {
            throw new RuntimeException("Unable to get theUnsafe.");
        }
    }
    public KeyEvent makeEvent(Component src, int keyCode, char keyChar, int location, int type, long time, int modifs) {
        KeyEvent event = new KeyEvent(src, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_E, 'e', KeyEvent.KEY_LOCATION_STANDARD);
        theUnsafe.putInt(event, offsetCode, keyCode);
        theUnsafe.putInt(event, offsetChar, keyChar);
        theUnsafe.putInt(event, offsetLocation, location);
        theUnsafe.putInt(event, offsetId, type);
        theUnsafe.putInt(event, offsetModifs, modifs);
        theUnsafe.putLong(event, offsetWhen, time);
        return event;
    }
}