/*
 * RBKernel.java - No description
 * Created on 10. September 2001, 17:56
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */

package org.jruby;

import java.io.*;
import java.net.*;

import org.jruby.*;
import org.jruby.exceptions.*;
import org.jruby.core.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RubyKernel {
    public static void createKernelModule(Ruby ruby) {
        RubyCallbackMethod sprintf = new ReflectionCallbackMethod(RubyKernel.class, "sprintf", true, true);
        RubyCallbackMethod p = new ReflectionCallbackMethod(RubyKernel.class, "p", true, true);
        RubyCallbackMethod print = new ReflectionCallbackMethod(RubyKernel.class, "print", true, true);
        RubyCallbackMethod printf = new ReflectionCallbackMethod(RubyKernel.class, "printf", true, true);
        RubyCallbackMethod puts = new ReflectionCallbackMethod(RubyKernel.class, "puts", true, true);
        RubyCallbackMethod raise = new ReflectionCallbackMethod(RubyKernel.class, "raise", true, true);
        RubyCallbackMethod require = new ReflectionCallbackMethod(RubyKernel.class, "require", RubyString.class, false, true);

        ruby.defineGlobalFunction("format", sprintf);
        ruby.defineGlobalFunction("p", p);
        ruby.defineGlobalFunction("print", print);
        ruby.defineGlobalFunction("printf", printf);
        ruby.defineGlobalFunction("puts", puts);
        ruby.defineGlobalFunction("raise", raise);
        ruby.defineGlobalFunction("require", require);
        ruby.defineGlobalFunction("singleton_method_added", DefaultCallbackMethods.getMethodNil());
        ruby.defineGlobalFunction("sprintf", sprintf);
    }

    public static RubyObject puts(Ruby ruby, RubyObject recv, RubyObject args[]) {
        if (args.length == 0) {
            ruby.getRuntime().getOutputStream().println();
            return ruby.getNil();
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                if (args[i] instanceof RubyArray) {
                    puts(ruby, recv, ((RubyArray) args[i]).toJavaArray());
                } else {
                    ruby.getRuntime().getOutputStream().println(
                        args[i].isNil() ? "nil" : ((RubyString) args[i].funcall(ruby.intern("to_s"))).getValue());
                }
            }
        }
        return ruby.getNil();
    }

    public static RubyObject raise(Ruby ruby, RubyObject recv, RubyObject args[]) {
        int argsLength = args != null ? args.length : 0;

        switch (argsLength) {
            case 0 :
            case 1 :
                throw new RaiseException(RubyException.s_new(ruby, ruby.getExceptions().getRuntimeError(), args));
            case 2 :
                RubyException excptn = (RubyException) args[0].funcall(ruby.intern("exception"), args[1]);
                throw new RaiseException(excptn);
            default :
                throw new RubyArgumentException(ruby, "wrong # of arguments");
        }
    }

    public static RubyObject print(Ruby ruby, RubyObject recv, RubyObject args[]) {
        RubyObject ofsObj = ruby.getGlobalVar("$,");
        RubyObject orsObj = ruby.getGlobalVar("$\\");
        String ofs = ofsObj.isNil() ? "" : RubyString.stringValue(ofsObj).getValue();
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                if (i > 0) {
                    ruby.getRuntime().getOutputStream().print(ofs);
                }
                ruby.getRuntime().getOutputStream().print(
                    args[i].isNil() ? "nil" : ((RubyString) args[i].funcall(ruby.intern("to_s"))).getValue());
            }
        }
        ruby.getRuntime().getOutputStream().print(orsObj.isNil() ? "" : RubyString.stringValue(orsObj).getValue());
        return ruby.getNil();
    }

    public static RubyObject p(Ruby ruby, RubyObject recv, RubyObject args[]) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                ruby.getRuntime().getOutputStream().println(((RubyString) args[i].funcall(ruby.intern("inspect"))).getValue());
            }
        }
        return ruby.getNil();
    }

    public static RubyObject sprintf(Ruby ruby, RubyObject recv, RubyObject args[]) {
        if (args.length == 0) {
            throw new RubyArgumentException(ruby, "sprintf must have at least one argument");
        }
        RubyString str = RubyString.stringValue(args[0]);
        RubyArray newArgs = RubyArray.m_create(ruby, null, args);
        newArgs.m_shift();
        return str.m_format(newArgs);
    }

    public static RubyObject printf(Ruby ruby, RubyObject recv, RubyObject args[]) {
        ruby.getRuntime().getOutputStream().print(((RubyString) sprintf(ruby, recv, args)).getValue());
        return ruby.getNil();
    }

    public static RubyObject require(Ruby ruby, RubyObject recv, RubyString arg1) {
        if (arg1.getValue().endsWith(".jar")) {
            File jarFile = new File(arg1.getValue());
            if (!jarFile.exists()) {
                jarFile = new File(new File(ruby.getSourceFile()).getParentFile(), arg1.getValue());
                if (!jarFile.exists()) {
                    ruby.getRuntime().getErrorStream().println("[Error] Jarfile + \"" + jarFile.getAbsolutePath() + "\"not found.");
                }
            }
            if (jarFile.exists()) {
                try {
                    ClassLoader javaClassLoader = new URLClassLoader(new URL[] { jarFile.toURL()}, ruby.getJavaClassLoader());
                    ruby.setJavaClassLoader(javaClassLoader);
                } catch (MalformedURLException murlExcptn) {
                }
            }
        } else {
            if (!arg1.getValue().endsWith(".rb")) {
                arg1 = RubyString.newString(ruby, arg1.getValue() + ".rb");
            }
            ruby.getRuntime().loadFile(arg1, false);
        }
        return ruby.getNil();
    }
}