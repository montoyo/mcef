package net.montoyo.mcef.coremod;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.*;

import java.util.Map;

@IFMLLoadingPlugin.Name(value = "ShutdownPatcher")
@IFMLLoadingPlugin.TransformerExclusions(value = "net.montoyo.mcef.")
@IFMLLoadingPlugin.SortingIndex(value = 90007531) //It has to run after the searge-name transformation
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class ShutdownPatcher implements IFMLLoadingPlugin, IClassTransformer {

    private static boolean PATCH_OK = false;
    private static final String OBF_SHUTDOWN_METHOD = "func_71405_e"; //The "searge-obfuscated" name of the Minecraft.shutdownMinecraftApplet() method

    public static boolean didPatchSucceed() {
        return PATCH_OK;
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { "net.montoyo.mcef.coremod.ShutdownPatcher" };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public byte[] transform(String name, String deobfName, byte[] cls) {
        if(!deobfName.equals("net.minecraft.client.Minecraft"))
            return cls;

        boolean envObf = !name.equals(deobfName); //If the current environment is obfuscated
        log("Now transforming %s, aka %s (obfuscated: %s)", name, deobfName, envObf ? "yes" : "no");

        try {
            ClassReader cr = new ClassReader(cls);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            McVisitor cv = new McVisitor(cw, envObf);
            cr.accept(cv, 0);

            return cw.toByteArray();
        } catch(Throwable t) {
            t.printStackTrace();
            log("Failed to setup Minecraft shutdown detector.");
        }

        return cls; //Abort class transforming
    }

    private static class ShutdownMCAppletVisitor extends MethodVisitor {

        public ShutdownMCAppletVisitor(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitCode() {
            mv.visitCode();
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "net/montoyo/mcef/MCEF", "onMinecraftShutdown", "()V", false);
            PATCH_OK = true;
            log("Target section has been patched.");
        }
    }

    private static class McVisitor extends ClassVisitor {

        private final boolean envObf;

        public McVisitor(ClassVisitor cv, boolean obf) {
            super(Opcodes.ASM5, cv);
            envObf = obf;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            if(access == Opcodes.ACC_PUBLIC && desc.equals("()V")) { //void shutdownMinecraftApplet()
                if((envObf && name.equals(OBF_SHUTDOWN_METHOD)) || name.equals("shutdownMinecraftApplet")) {
                    log("shutdownMinecraftApplet() method found; transforming...");
                    return new ShutdownMCAppletVisitor(mv);
                }
            }

            return mv;
        }

    }

    private static void log(String str, Object ... args) {
        LogManager.getLogger("MCEF").log(Level.INFO, String.format(str, args));
    }

}
