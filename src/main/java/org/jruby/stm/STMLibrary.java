package org.jruby.stm;

import clojure.lang.LockingTransaction;
import clojure.lang.Ref;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;

import java.io.IOException;
import java.util.concurrent.Callable;

public class STMLibrary implements Library {
    public void load(Ruby runtime, boolean wrap) throws IOException {
        RubyModule stmModule = runtime.defineModule("STM");
        RubyClass refClass = runtime.defineClassUnder("Ref", runtime.getObject(), new RefAllocator(), stmModule);

        refClass.defineAnnotatedMethods(Ref.class);
        stmModule.getSingletonClass().defineAnnotatedMethods(Dosync.class);
    }

    public static class RefAllocator implements ObjectAllocator {
        public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
            try {
                return new Ref(runtime, klazz);
            } catch (Exception e) {
                // does this ever happen?
                throw runtime.newRuntimeError(e.getLocalizedMessage());
            }
        }
    }

    public static class Ref extends RubyObject {
        private final clojure.lang.Ref ref;

        public Ref(Ruby runtime, RubyClass cls) throws Exception {
            super(runtime, cls);
            ref = new clojure.lang.Ref(runtime.getNil());
        }

        @JRubyMethod
        public IRubyObject get() {
            return (IRubyObject)ref.deref();
        }

        @JRubyMethod
        public IRubyObject set(IRubyObject value) {
            return (IRubyObject)ref.set(value);
        }
    }

    public static class Dosync {
        @JRubyMethod
        public static IRubyObject dosync(final ThreadContext context, final IRubyObject self, final Block block) throws Exception {
            final Ruby ruby = context.getRuntime();

            return (IRubyObject) LockingTransaction.runInTransaction(new Callable() {
                public Object call() throws Exception {
                    // re-get transaction in case this gets run in different threads
                    return block.call(ruby.getCurrentContext());
                }
            });
        }
    }
}
