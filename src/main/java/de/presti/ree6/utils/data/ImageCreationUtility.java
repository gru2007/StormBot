package de.presti.ree6.utils.data;

import de.presti.ree6.sql.entities.UserLevel;
import net.dv8tion.jda.api.entities.User;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ImageCreationUtility {

    /**
     * Constructor for the Image Creation Utility class.
     */
    private ImageCreationUtility() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Generate a Rank Image with Java native Graphics2D.
     *
     * @param userLevel the User Level Object.
     * @return the bytes of the Image.
     * @throws IOException when URL-Format is Invalid or the URL is not a valid Image.
     */
    public static byte[] createRankImage(UserLevel userLevel) throws IOException {
        if (userLevel == null || userLevel.getUser() == null)
            return new byte[128];

        // Generate a 885x211 Image Background.
        BufferedImage base = ImageIO.read(new File("images/base.png"));

        User user = userLevel.getUser();
        BufferedImage userImage;

        // Generated a Circle Image with the Avatar of the User.
        if (user.getAvatarUrl() != null) {
            userImage = convertToCircleShape(new URL(user.getAvatarUrl()));
        } else {
            userImage = convertToCircleShape(new URL(user.getDefaultAvatarUrl()));
        }

        // Create a new Graphics2D instance from the base.
        Graphics2D graphics2D = base.createGraphics();

        // Change the background to black.

        // Draw basic Information, such as the User Image, Username and the Experience Rect.
        graphics2D.drawImage(userImage, null, 175, 450);
        graphics2D.setColor(Color.WHITE);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Font verdana60 = new Font("Verdana", Font.PLAIN, 60);
        Font verdana50 = new Font("Verdana", Font.PLAIN, 50);
        Font verdana40 = new Font("Verdana", Font.PLAIN, 40);

        String username = new String(user.getName().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        if (username.length() > 13) {
            username = username.substring(0, 12);
        }

        graphics2D.setFont(verdana60);
        graphics2D.drawString(username, 425, 675);
        graphics2D.setColor(Color.LIGHT_GRAY);
        graphics2D.setFont(verdana40);
        graphics2D.drawString("#" + user.getDiscriminator(), 425, 675 - graphics2D.getFontMetrics(verdana60).getHeight() + 5);
        graphics2D.setColor(Color.magenta.darker().darker());
        graphics2D.fillRoundRect(175, 705, base.getWidth() - 950, 50, 50, 50);

        //region Experience

        // Draw The current Experience and needed Experience for the next Level.
        graphics2D.setColor(Color.LIGHT_GRAY);
        graphics2D.setFont(verdana40);
        graphics2D.drawString(userLevel.getFormattedExperience() + "", (base.getWidth() - 800) - (graphics2D.getFontMetrics().stringWidth("/" + userLevel.getFormattedExperience(userLevel.getExperienceForNextLevel()))) - 5 - graphics2D.getFontMetrics().stringWidth(userLevel.getFormattedExperience() + ""), 675);
        graphics2D.setColor(Color.GRAY);
        graphics2D.drawString("/" + userLevel.getFormattedExperience(userLevel.getTotalExperienceForNextLevel()), (base.getWidth() - 800) - (graphics2D.getFontMetrics().stringWidth("/" + userLevel.getFormattedExperience(userLevel.getExperienceForNextLevel()))), 675);

        //endregion

        //region Rank

        // Draw the current Ranking.
        graphics2D.setColor(Color.WHITE);
        graphics2D.setFont(verdana40);
        graphics2D.drawString("Rank", (base.getWidth() - 800) - (graphics2D.getFontMetrics(verdana50).stringWidth("" + userLevel.getRank())) - (graphics2D.getFontMetrics().stringWidth("Rank")) - 10, 675 - graphics2D.getFontMetrics().getHeight() - graphics2D.getFontMetrics().getHeight());

        graphics2D.setColor(Color.MAGENTA.brighter());
        graphics2D.setFont(verdana50);
        graphics2D.drawString("" + userLevel.getRank(), (base.getWidth() - 800) - (graphics2D.getFontMetrics().stringWidth("" + userLevel.getRank())), 675 - graphics2D.getFontMetrics(verdana40).getHeight() - graphics2D.getFontMetrics(verdana40).getHeight());

        //endregion

        //region Level

        // Draw the current Level.
        graphics2D.setColor(Color.WHITE);
        graphics2D.setFont(verdana40);
        graphics2D.drawString("Level", (base.getWidth() - 800) - (graphics2D.getFontMetrics(verdana50).stringWidth("" + userLevel.getLevel())) - (graphics2D.getFontMetrics().stringWidth("Level")) - 10, 675 - graphics2D.getFontMetrics().getHeight());

        graphics2D.setColor(Color.magenta.brighter());
        graphics2D.setFont(verdana50);
        graphics2D.drawString("" + userLevel.getLevel(), (base.getWidth() - 800) - (graphics2D.getFontMetrics().stringWidth("" + userLevel.getLevel())), 675 - graphics2D.getFontMetrics(verdana40).getHeight());

        //endregion

        // Draw the Progressbar.
        graphics2D.setColor(Color.magenta);
        graphics2D.fillRoundRect(175, 705, (base.getWidth() - 950) * (int) userLevel.getProgress() / 100, 50, 50, 50);

        // Close the Graphics2d instance.
        graphics2D.dispose();

        // Create a ByteArrayOutputStream to convert the BufferedImage to an Array of Bytes.
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ImageIO.write(base, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Generate a HornyJail Image with Java native Graphics2D.
     *
     * @param user the User Object.
     * @return the bytes of the Image.
     * @throws IOException when URL-Format is Invalid or the URL is not a valid Image.
     */
    public static byte[] createHornyJailImage(User user) throws IOException {
        if (user == null)
            return new byte[128];

        // Generate a 128x128 Image Background.
        BufferedImage base = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        BufferedImage userImage;

        // Generated a Circle Image with the Avatar of the User.
        if (user.getAvatarUrl() != null) {
            userImage = ImageIO.read(new URL(user.getAvatarUrl()));
        } else {
            userImage = ImageIO.read(new URL(user.getDefaultAvatarUrl()));
        }

        userImage = resize(userImage, 128, 128);

        // Create a new Graphics2D instance from the base.
        Graphics2D graphics2D = base.createGraphics();

        // Change the background to black.
        graphics2D.setColor(Color.BLACK);
        graphics2D.fillRoundRect(0, 0, base.getWidth(), base.getHeight(), 10, 10);

        // Draw basic Information, such as the User Image, Username and the Experience Rect.
        graphics2D.drawImage(userImage, null, 0, 0);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int xOffset = base.getWidth() / 7;

        for (int i = -1; i < 7; i++) {
            graphics2D.setColor(Color.DARK_GRAY);
            graphics2D.drawRect((xOffset * i) + xOffset, -1, 4, 130);
            graphics2D.setColor(Color.GRAY);
            graphics2D.fillRect((xOffset * i) + xOffset + 1, -1, 3, 130);
        }

        // Close the Graphics2d instance.
        graphics2D.dispose();

        // Create a ByteArrayOutputStream to convert the BufferedImage to an Array of Bytes.
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ImageIO.write(base, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Generated a Circle Shaped Version of the given Image.
     *
     * @param url the URL to the Image.
     * @return the edited {@link BufferedImage}.
     * @throws IOException if the link is not a valid Image.
     */
    public static BufferedImage convertToCircleShape(URL url) throws IOException {

        BufferedImage mainImage = ImageIO.read(url);

        BufferedImage output = new BufferedImage(mainImage.getWidth(), mainImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = output.createGraphics();

        // This is what we want, but it only does hard-clipping, i.e. aliasing
        // g2.setClip(new RoundRectangle2D ...)

        // so instead fake soft-clipping by first drawing the desired clip shape
        // in fully opaque white with antialiasing enabled...
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Float(0, 0, mainImage.getHeight(), mainImage.getHeight()));

        // ... then compositing the image on top,
        // using the white shape from above as alpha source
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(mainImage, 0, 0, null);

        g2.dispose();

        return resize(output, 250, 250);
    }

    /**
     * Resizes an image to an absolute width and height (the image may not be
     * proportional)
     * @param inputImage The original image
     * @param scaledWidth absolute width in pixels
     * @param scaledHeight absolute height in pixels
     */
    public static BufferedImage resize(BufferedImage inputImage, int scaledWidth, int scaledHeight){

        // creates output image
        BufferedImage outputImage = new BufferedImage(scaledWidth,
                scaledHeight, inputImage.getType());

        // scales the input image to the output image
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        return outputImage;
    }

}
