package id.itemforge;

import java.util.List;

public record Ability(String key, Trigger trigger, double cooldown, String permission, boolean consume, List<String> actions) {}
