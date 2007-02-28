/*
 * Copyright (c) 1998-2002 Carnegie Mellon University.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY CARNEGIE MELLON UNIVERSITY ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package rcm.util;

import java.lang.reflect.*;

/**
 * Handy reflection routines.
 */
public abstract class Reflect {
    /**
     * Create a new instance of a class by calling a constructor with 
     * arguments.
     */
    public static Object newInstance (String className, 
                                      Class[] signature,
                                      Object[] args) 
        throws Exception {
        
        Class cls = Class.forName (className);
        Constructor constructor = cls.getConstructor (signature);
        return constructor.newInstance (args);
    }

    /**
     * Call a method of an object.
     */
    public static Object callMethod (Object obj, 
                                     String methodName,
                                     Class[] signature,
                                     Object[] args) 
        throws Exception {

        Class cls = obj.getClass ();
        Method method = cls.getMethod (methodName, signature);
        return method.invoke (obj, args);
    }

    /**
     * Call a static method of a class.
     */
    public static Object callStaticMethod (String className, 
                                           String methodName,
                                           Class[] signature,
                                           Object[] args) 
        throws Exception {
        
        Class cls = Class.forName (className);
        Method method = cls.getMethod (methodName, signature);
        return method.invoke (cls, args);
    }
}

