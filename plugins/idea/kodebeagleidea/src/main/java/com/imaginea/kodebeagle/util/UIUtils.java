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

import com.imaginea.kodebeagle.object.WindowObjects;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;

public class UIUtils {
    private final WindowObjects windowObjects = WindowObjects.getInstance();

    public final void showHelpInfo(final String info) {
        JPanel centerInfoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerInfoPanel.add(new JLabel(info));
        goToAllPane();
        windowObjects.getjTreeScrollPane().setViewportView(centerInfoPanel);
    }

    public final void goToAllPane() {
        windowObjects.getjTabbedPane().setSelectedIndex(1);
    }

    public final void goToSpotlightPane() {
        windowObjects.getjTabbedPane().setSelectedIndex(0);
    }
}
