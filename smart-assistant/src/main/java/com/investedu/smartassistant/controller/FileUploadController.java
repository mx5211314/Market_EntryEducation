package com.investedu.smartassistant.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    private static final Set<String> ALLOWED_EXTS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");

    // 封面在列表和详情页都按 16:9 展示，服务端统一裁到这个比例，避免各种奇怪尺寸把版式撑歪
    private static final int COVER_W = 1200;
    private static final int COVER_H = 675;
    // 正文插图不裁比例，只限制最大宽度，防止一张 6000px 的原图拖慢页面
    private static final int INLINE_MAX_W = 1600;

    @PostMapping
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(defaultValue = "false") boolean cover) {
        if (file == null || file.isEmpty()) {
            return fail("文件为空");
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf(".")).toLowerCase();
        }
        if (!ALLOWED_EXTS.contains(ext)) {
            return fail("不支持的图片格式：" + (ext.isEmpty() ? "无扩展名" : ext));
        }

        try {
            // transferTo 收到相对路径时会相对 Tomcat 的临时工作目录解析，必须先转绝对路径
            File dir = new File(uploadDir).getAbsoluteFile();
            if (!dir.exists() && !dir.mkdirs()) {
                return fail("无法创建上传目录：" + dir.getPath());
            }

            String base = UUID.randomUUID().toString().replace("-", "");
            String filename = normalize(file, dir, base, ext, cover);

            Map<String, Object> data = new HashMap<>();
            data.put("url", "/uploads/" + filename);
            data.put("alt", "");
            data.put("href", "");
            return Map.of("errno", 0, "msg", "上传成功", "data", data);
        } catch (Exception e) {
            return fail("上传失败：" + e.getMessage());
        }
    }

    // ImageIO 不认 webp/部分 bmp，这时 read 返回 null，原样落盘不做处理
    private String normalize(MultipartFile file, File dir, String base, String ext, boolean cover) throws Exception {
        BufferedImage src = ImageIO.read(file.getInputStream());
        if (src == null) {
            String name = base + ext;
            file.transferTo(new File(dir, name));
            return name;
        }

        BufferedImage out = cover ? cropToCover(src) : shrinkIfWide(src);
        if (out == src) {
            String name = base + ext;
            file.transferTo(new File(dir, name));
            return name;
        }

        String name = base + ".jpg";
        ImageIO.write(out, "jpg", new File(dir, name));
        return name;
    }

    // 先按 16:9 从中心裁掉多余的边，再缩放到统一尺寸，构图不会被拉变形
    private BufferedImage cropToCover(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int cropW = w;
        int cropH = w * COVER_H / COVER_W;
        if (cropH > h) {
            cropH = h;
            cropW = h * COVER_W / COVER_H;
        }
        BufferedImage cropped = src.getSubimage((w - cropW) / 2, (h - cropH) / 2, cropW, cropH);
        return draw(cropped, COVER_W, COVER_H);
    }

    private BufferedImage shrinkIfWide(BufferedImage src) {
        if (src.getWidth() <= INLINE_MAX_W) return src;
        int h = src.getHeight() * INLINE_MAX_W / src.getWidth();
        return draw(src, INLINE_MAX_W, h);
    }

    // 统一转成 RGB 画布，否则带透明通道的 PNG 存成 JPEG 会整片发黑
    private BufferedImage draw(BufferedImage src, int w, int h) {
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }

    // 超过 multipart 限制时异常在进入方法前就抛出，这里兜住并给出可读原因
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Map<String, Object> handleTooLarge() {
        return fail("图片超过 5MB，请压缩后再上传");
    }

    // wangEditor 约定 errno != 0 即失败，前端读 msg 展示
    private Map<String, Object> fail(String msg) {
        return Map.of("errno", 1, "msg", msg);
    }
}