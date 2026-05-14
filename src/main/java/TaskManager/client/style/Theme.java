package TaskManager.client.style;

public class Theme {

    // -------------------------------------------------------------------------
    // Couleurs
    // -------------------------------------------------------------------------
    public static final String PRIMARY      = "#185FA5";
    public static final String PRIMARY_LIGHT= "#E6F1FB";
    public static final String DANGER       = "#A32D2D";
    public static final String DANGER_LIGHT = "#FCEBEB";
    public static final String SUCCESS      = "#3B6D11";
    public static final String WARNING      = "#BA7517";
    public static final String MUTED       = "#888780";
    public static final String BG_PRIMARY   = "#FFFFFF";
    public static final String BG_SECONDARY = "#F1EFE8";
    public static final String BORDER       = "#D3D1C7";
    public static final String TEXT_PRIMARY = "#2C2C2A";
    public static final String TEXT_SECONDARY = "#5F5E5A";

    // -------------------------------------------------------------------------
    // Styles boutons
    // -------------------------------------------------------------------------
    public static final String BTN_PRIMARY =
            "-fx-background-color: " + PRIMARY + ";" +
                    "-fx-text-fill: " + PRIMARY_LIGHT + ";" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 8 16 8 16;" +
                    "-fx-font-size: 13px;" +
                    "-fx-cursor: hand;";

    public static final String BTN_OUTLINE =
            "-fx-background-color: transparent;" +
                    "-fx-text-fill: " + PRIMARY + ";" +
                    "-fx-border-color: " + PRIMARY + ";" +
                    "-fx-border-radius: 8;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 8 16 8 16;" +
                    "-fx-font-size: 13px;" +
                    "-fx-cursor: hand;";

    public static final String BTN_DISABLED =
            "-fx-background-color: " + BG_SECONDARY + ";" +
                    "-fx-text-fill: " + MUTED + ";" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 8 16 8 16;" +
                    "-fx-font-size: 13px;" +
                    "-fx-cursor: default;";

    // -------------------------------------------------------------------------
    // Styles champs de saisie
    // -------------------------------------------------------------------------
    public static final String INPUT =
            "-fx-background-color: " + BG_SECONDARY + ";" +
                    "-fx-border-color: " + BORDER + ";" +
                    "-fx-border-radius: 8;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 7 10 7 10;" +
                    "-fx-font-size: 13px;" +
                    "-fx-text-fill: " + TEXT_PRIMARY + ";";

    // -------------------------------------------------------------------------
    // Styles labels
    // -------------------------------------------------------------------------
    public static final String LABEL_TITLE =
            "-fx-font-size: 16px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: " + TEXT_PRIMARY + ";";

    public static final String LABEL_SUBTITLE =
            "-fx-font-size: 12px;" +
                    "-fx-text-fill: " + TEXT_SECONDARY + ";";

    public static final String LABEL_SECTION =
            "-fx-font-size: 11px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: " + TEXT_SECONDARY + ";";

    public static final String LABEL_DANGER =
            "-fx-font-size: 12px;" +
                    "-fx-text-fill: " + DANGER + ";";

    // -------------------------------------------------------------------------
    // Styles conteneurs
    // -------------------------------------------------------------------------
    public static final String CARD =
            "-fx-background-color: " + BG_PRIMARY + ";" +
                    "-fx-border-color: " + BORDER + ";" +
                    "-fx-border-radius: 12;" +
                    "-fx-background-radius: 12;" +
                    "-fx-padding: 16;";

    public static final String SIDEBAR =
            "-fx-background-color: " + BG_SECONDARY + ";" +
                    "-fx-border-color: " + BORDER + ";" +
                    "-fx-pref-width: 160px;";

    public static final String SIDEBAR_ITEM =
            "-fx-background-color: transparent;" +
                    "-fx-text-fill: " + TEXT_SECONDARY + ";" +
                    "-fx-font-size: 12px;" +
                    "-fx-padding: 7 12 7 12;" +
                    "-fx-cursor: hand;" +
                    "-fx-alignment: center-left;";

    public static final String SIDEBAR_ITEM_ACTIVE =
            "-fx-background-color: " + BG_PRIMARY + ";" +
                    "-fx-text-fill: " + PRIMARY + ";" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 7 12 7 14;" +
                    "-fx-cursor: hand;" +
                    "-fx-alignment: center-left;" +
                    "-fx-border-color: transparent transparent transparent " + PRIMARY + ";" +
                    "-fx-border-width: 0 0 0 2;";
}