package net.kunmc.lab.humanslingshot.util;

import org.bukkit.util.Vector;

public class VectorUtil {
    public static Vector toUnit(Vector vector) {
        double length = vector.length();
        return vector.clone().divide(new Vector(length, length, length));
    }
}