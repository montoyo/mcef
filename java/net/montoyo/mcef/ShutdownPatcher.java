package net.montoyo.mcef;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraft.launchwrapper.IClassTransformer;
import net.montoyo.mcef.utilities.Log;
import org.objectweb.asm.*;

import java.util.Map;

@IFMLLoadingPlugin.Name(value = "ShutdownPatcher")
@IFMLLoadingPlugin.TransformerExclusions(value = "net.montoyo.mcef.")
@IFMLLoadingPlugin.SortingIndex(value = 90007531) //It has to run after the searge-name transformation
public class ShutdownPatcher implements IFMLLoadingPlugin, IClassTransformer {

    private static boolean PATCH_OK = false;
    private static final String OBF_RUN_METHOD = "func_99999_d"; //The "searge-obfuscated" name of the Minecraft.run() method
    private static final String OBF_RUNNING_FIELD = "field_71425_J"; //The "searge-obfuscated" name of the Minecraft.running field

    public static boolean didPatchSucceed() {
        return PATCH_OK;
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { "net.montoyo.mcef.ShutdownPatcher" };
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
        Log.info("Now transforming %s, aka %s (obfuscated: %s)", name, deobfName, envObf ? "yes" : "no");

        try {
            ClassReader cr = new ClassReader(cls);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            McVisitor cv = new McVisitor(cw, envObf);
            cr.accept(cv, 0);

            return cw.toByteArray();
        } catch(Throwable t) {
            t.printStackTrace();
            Log.error("Failed to setup Minecraft shutdown detector.");
        }

        return cls; //Abort class transforming
    }

    private static class RunVisitor extends MethodVisitor {

        private boolean patched;
        private final boolean envObf;

        public RunVisitor(MethodVisitor mv, boolean obf) {
            super(Opcodes.ASM5, mv);

            patched = false;
            envObf = obf;
        }

        @Override
        public void visitFrame(int type, int numLocals, Object[] locals, int numStack, Object[] stack) {
            mv.visitFrame(type, numLocals, locals, numStack, stack);

            if(!patched && type == Opcodes.F_SAME && numLocals == 0 && numStack == 0) {
                //Here we patch!
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETFIELD, "net/minecraft/client/Minecraft", envObf ? OBF_RUNNING_FIELD : "running", "Z");

                Label patchEnd = new Label();
                mv.visitJumpInsn(Opcodes.IFNE, patchEnd);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "net/montoyo/mcef/MCEF", "onMinecraftShutdown", "()V", false);
                mv.visitLabel(patchEnd);

                patched = true;
                PATCH_OK = true;
                Log.info("Target section has been patched.");
            }
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
            if(access == Opcodes.ACC_PUBLIC && desc.equals("()V")) { //void run()
                if((envObf && name.equals(OBF_RUN_METHOD)) || name.equals("run")) {
                    Log.info("run() method found; transforming...");
                    return new RunVisitor(mv, envObf);
                }
            }

            return mv;
        }

    }

}
