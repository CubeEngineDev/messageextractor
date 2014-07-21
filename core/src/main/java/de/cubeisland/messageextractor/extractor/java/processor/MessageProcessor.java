/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Phillip Schichtel, Stefan Wolf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.cubeisland.messageextractor.extractor.java.processor;

import de.cubeisland.messageextractor.extractor.java.JavaExtractorConfiguration;
import de.cubeisland.messageextractor.message.MessageStore;
import de.cubeisland.messageextractor.message.Occurrence;
import de.cubeisland.messageextractor.util.Misc;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.CtElement;
import spoon.support.reflect.code.CtBinaryOperatorImpl;
import spoon.support.reflect.code.CtLiteralImpl;

public abstract class MessageProcessor<E extends CtElement> extends AbstractProcessor<E>
{
    private final JavaExtractorConfiguration configuration;
    private final MessageStore messageStore;

    public MessageProcessor(JavaExtractorConfiguration configuration, MessageStore messageStore)
    {
        this.configuration = configuration;
        this.messageStore = messageStore;
    }

    public JavaExtractorConfiguration getConfiguration()
    {
        return configuration;
    }

    public MessageStore getMessageStore()
    {
        return messageStore;
    }

    public void addMessage(E element, String singular, String plural)
    {
        Occurrence occurrence = new Occurrence(Misc.getRelativizedFile(this.getConfiguration().getDirectory(), element.getPosition().getFile()), element.getPosition().getLine());
        this.getMessageStore().addMessage(singular, plural, occurrence);
    }

    protected String getString(CtExpression<?> expression)
    {
        if (expression instanceof CtLiteralImpl<?>)
        {
            return (String) ((CtLiteralImpl<?>) expression).getValue();
        }
        else if (expression instanceof CtBinaryOperatorImpl<?>)
        {
            return this.getString((CtBinaryOperatorImpl<?>) expression);
        }

        System.out.println("The Expression '" + expression.getClass().getName() + "' isn't supported yet.");
        return null;
    }

    protected String getString(CtBinaryOperatorImpl<?> expression)
    {
        StringBuilder value = new StringBuilder(2);

        if (!BinaryOperatorKind.PLUS.equals(expression.getKind()))
        {
            System.out.println("Just the '+' binary operator can be used for string operations. '" + expression.getKind().name() + "' isn't supported.");
            return null;
        }

        String string = this.getString(expression.getLeftHandOperand());
        if (string == null)
        {
            return null;
        }
        value.append(string);

        string = this.getString(expression.getRightHandOperand());
        if (string == null)
        {
            return null;
        }
        value.append(string);

        return value.toString();
    }
}