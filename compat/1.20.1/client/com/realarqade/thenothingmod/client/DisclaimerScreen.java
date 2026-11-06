package com.realarqade.thenothingmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The content warning shown once each time the game starts (before the title screen). Two pages: the content warning, then the
 * disclaimer (what the mod is, that it makes files, and so on). The checkbox and the buttons that leave are on the second page.
 * <ul>
 *   <li><b>Release</b>: it can't be skipped. Escape does nothing, and "Done" and "Don't show again" stay greyed out until the
 *   checkbox is ticked.</li>
 *   <li><b>Development environment</b> (launching from the IDE): SPACE or Escape skips it, and the buttons always work.</li>
 *   <li>"Don't show again" (both) stores that in the config. It can be switched back on in the config screen.</li>
 * </ul>
 * The wording is in the lang file ({@code disclaimer.the-nothing-mod.*}). In the page-two bullets, text between {@code **} is shown in bold.
 */
final class DisclaimerScreen extends DustScreen {
    @Override
    protected boolean customDraw() {
        return false;
    }

    private static final int BULLETS = 6;

    private final Screen next;
    private final boolean development = DustDevTools.inDevelopmentEnvironment();
    private int page;
    private boolean acknowledged;
    private Button done;
    private Button dontShowAgain;

    DisclaimerScreen(Screen next) {
        super(Component.translatable("disclaimer.the-nothing-mod.title"));
        this.next = next;
    }

    @Override
    protected void init() {
        done = null;
        dontShowAgain = null;

        int contentWidth = Math.min(this.width - 40, 380);
        int x = (this.width - contentWidth) / 2;
        boolean lastPage = page == 1;

        Component heading = Component.translatable(lastPage ? "disclaimer.the-nothing-mod.page2.title" : "disclaimer.the-nothing-mod.title")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.RED);
        Component body = lastPage ? bullets() : Component.translatable("disclaimer.the-nothing-mod.body");
        Component hint = Component.translatable("disclaimer.the-nothing-mod.dev_hint").withStyle(ChatFormatting.GRAY);

        // Work out the heights first, so the whole block sits in the middle of the screen.
        int headingHeight = font.split(heading, contentWidth).size() * font.lineHeight;
        int bodyHeight = font.split(body, contentWidth).size() * font.lineHeight;
        int hintHeight = development ? font.lineHeight + 8 : 0;
        int checkboxHeight = lastPage ? 30 : 0;
        int buttonHeight = 20;
        int total = headingHeight + 10 + bodyHeight + 12 + hintHeight + checkboxHeight + buttonHeight;
        int y = Math.max(8, (this.height - total) / 2);

        addRenderableWidget(new MultiLineTextWidget(x, y, heading, font).setMaxWidth(contentWidth).setCentered(true));
        y += headingHeight + 10;
        addRenderableWidget(new MultiLineTextWidget(x, y, body, font).setMaxWidth(contentWidth));
        y += bodyHeight + 12;
        if (development) {
            addRenderableWidget(new MultiLineTextWidget(x, y, hint, font).setMaxWidth(contentWidth));
            y += hintHeight;
        }

        if (!lastPage) {
            addRenderableWidget(Button.builder(Component.translatable("disclaimer.the-nothing-mod.next"), button -> showPage(1))
                    .bounds(x, y, contentWidth, buttonHeight)
                    .build());
            return;
        }

        Checkbox acknowledge = new Checkbox(x, y, contentWidth, 20, Component.translatable("disclaimer.the-nothing-mod.acknowledge"), acknowledged) {
            @Override
            public void onPress() {
                super.onPress();
                acknowledged = selected();
                updateButtons();
            }
        };
        addRenderableWidget(acknowledge);
        y += checkboxHeight;

        int third = (contentWidth - 16) / 3;
        addRenderableWidget(Button.builder(Component.translatable("disclaimer.the-nothing-mod.back"), button -> showPage(0))
                .bounds(x, y, third, buttonHeight)
                .build());
        done = Button.builder(Component.translatable("gui.done"), button -> finish(false))
                .bounds(x + third + 8, y, third, buttonHeight)
                .build();
        dontShowAgain = Button.builder(Component.translatable("disclaimer.the-nothing-mod.dont_show_again"), button -> finish(true))
                .bounds(x + 2 * (third + 8), y, third, buttonHeight)
                .build();
        addRenderableWidget(done);
        addRenderableWidget(dontShowAgain);
        updateButtons();
    }

    private void showPage(int newPage) {
        page = newPage;
        rebuildWidgets();
    }

    /** The disclaimer bullets from the lang file, each on its own line, with **text** shown in bold. */
    private static Component bullets() {
        MutableComponent all = Component.empty();
        for (int i = 0; i < BULLETS; i++) {
            if (i > 0) {
                all.append("\n\n");
            }
            all.append("- ").append(markup(Component.translatable("disclaimer.the-nothing-mod.bullet." + i).getString()));
        }
        return all;
    }

    private static MutableComponent markup(String text) {
        MutableComponent result = Component.empty();
        String[] parts = text.split("\\*\\*", -1);
        for (int i = 0; i < parts.length; i++) {
            MutableComponent part = Component.literal(parts[i]);
            result.append(i % 2 == 1 ? part.withStyle(ChatFormatting.BOLD) : part);
        }
        return result;
    }

    /** In release, nothing gets past this screen until the box is ticked. In development it never blocks. */
    private void updateButtons() {
        if (done == null || dontShowAgain == null) {
            return;
        }
        boolean allowed = development || acknowledged;
        done.active = allowed;
        dontShowAgain.active = allowed;
    }

    private void finish(boolean dontShowAgainFromNowOn) {
        if (dontShowAgainFromNowOn) {
            DustConfig.get().showDisclaimer = false;
            DustConfig.HANDLER.save();
        }
        minecraft.setScreen(next);
    }

    @Override
    protected boolean onKey(int key) {
        if (development && key == InputConstants.KEY_SPACE) {
            finish(false);
            return true;
        }
        return false;
    }

    /** Escape skips it in development only. */
    @Override
    public boolean shouldCloseOnEsc() {
        return development;
    }

    @Override
    public void onClose() {
        finish(false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
