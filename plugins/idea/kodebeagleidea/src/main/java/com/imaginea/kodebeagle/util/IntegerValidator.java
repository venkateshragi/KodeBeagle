/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.imaginea.kodebeagle.util;

import java.awt.event.KeyEvent;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class IntegerValidator extends AbstractValidator {

    private static final String ONLY_DIGITS = " can have only digits [0-9]!";

    public IntegerValidator(final String nameOfField,
                            final JTextField jTextField,
                            final JLabel validationLabel) {
        jTextField.addKeyListener(this);
        this.setjTextField(jTextField);
        this.setNameOfField(nameOfField);
        this.setValidationLabel(validationLabel);

    }

    public final boolean validateInteger(final String integer) {
        boolean matches = integer.matches("^\\d+$");
        return matches;
    }

    @Override
    public void keyTyped(final KeyEvent e) {

    }

    @Override
    public void keyPressed(final KeyEvent e) {

    }

    @Override
    public final void keyReleased(final KeyEvent e) {
        if (!validateInteger(this.getjTextField().getText())) {
            if (this.getjTextField().getText().equals("")) {
                this.getValidationLabel().setText(this.getNameOfField() + NOT_EMPTY);
            } else {
                this.getValidationLabel().setText(this.getNameOfField()
                        + ONLY_DIGITS);
            }
        } else {
            this.getValidationLabel().setText("");
        }
    }
}
