/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * General functions useful to all widgets that create preference blocks
 * 
 * @since 3.8
 */
public abstract class ConfigurationBlock {

	private static final int HIGHLIGHT_FOCUS = SWT.COLOR_WIDGET_DARK_SHADOW;
	private static final int HIGHLIGHT_MOUSE = SWT.COLOR_WIDGET_NORMAL_SHADOW;
	private static final int HIGHLIGHT_NONE = SWT.NONE;
	
	protected void addHighlight(final Composite parent, final Label labelControl, final Combo comboBox) {
		comboBox.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
				highlight(parent, labelControl, comboBox, HIGHLIGHT_NONE);
			}
			public void focusGained(FocusEvent e) {
				highlight(parent, labelControl, comboBox, HIGHLIGHT_FOCUS);
			}
		});
		
		MouseTrackAdapter labelComboListener= new MouseTrackAdapter() {
			public void mouseEnter(MouseEvent e) {
				highlight(parent, labelControl, comboBox, comboBox.isFocusControl() ? HIGHLIGHT_FOCUS : HIGHLIGHT_MOUSE);
			}
			public void mouseExit(MouseEvent e) {
				if (! comboBox.isFocusControl())
					highlight(parent, labelControl, comboBox, HIGHLIGHT_NONE);
			}
		};
		comboBox.addMouseTrackListener(labelComboListener);
		labelControl.addMouseTrackListener(labelComboListener);
		
		class MouseMoveTrackListener extends MouseTrackAdapter implements MouseMoveListener, MouseListener {
			public void mouseExit(MouseEvent e) {
				if (! comboBox.isFocusControl())
					highlight(parent, labelControl, comboBox, HIGHLIGHT_NONE);
			}
			public void mouseMove(MouseEvent e) {
				int color= comboBox.isFocusControl() ? HIGHLIGHT_FOCUS : isAroundLabel(e) ? HIGHLIGHT_MOUSE : HIGHLIGHT_NONE;
				highlight(parent, labelControl, comboBox, color);
			}
			public void mouseDown(MouseEvent e) {
				if (isAroundLabel(e))
					comboBox.setFocus();
			}
			public void mouseDoubleClick(MouseEvent e) {
				// not used
			}
			public void mouseUp(MouseEvent e) {
				// not used
			}
			private boolean isAroundLabel(MouseEvent e) {
				int lx= labelControl.getLocation().x;
				Rectangle c= comboBox.getBounds();
				int x= e.x;
				int y= e.y;
				boolean isAroundLabel= lx - 5 < x && x < c.x && c.y - 2 < y && y < c.y + c.height + 2;
				return isAroundLabel;
			}
		}
		MouseMoveTrackListener parentListener= new MouseMoveTrackListener();
		parent.addMouseMoveListener(parentListener);
		parent.addMouseTrackListener(parentListener);
		parent.addMouseListener(parentListener);
		
		MouseAdapter labelClickListener= new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				comboBox.setFocus();
			}
		};
		labelControl.addMouseListener(labelClickListener);
	}
	
	protected void highlight(final Composite parent, final Label labelControl, final Combo comboBox, final int color) {
		
		class HighlightPainter implements PaintListener {
			
			private int fColor= color;

			public void paintControl(PaintEvent e) {
				if (((GridData) labelControl.getLayoutData()).exclude) {
					parent.removePaintListener(this);
					labelControl.setData(null);
					return;
				}
				
				int GAP= 7;
				int ARROW= 3;
				Rectangle l= labelControl.getBounds();
				Point c= comboBox.getLocation();
				
				e.gc.setForeground(e.display.getSystemColor(fColor));
				int x2= c.x - GAP;
				int y= l.y + l.height / 2 + 1;
				
				e.gc.drawLine(l.x + l.width + GAP, y, x2, y);
				e.gc.drawLine(x2 - ARROW, y - ARROW, x2, y);
				e.gc.drawLine(x2 - ARROW, y + ARROW, x2, y);
			}
		}
		
		Object data= labelControl.getData();
		if (data == null) {
			if (color != HIGHLIGHT_NONE) {
				PaintListener painter= new HighlightPainter();
				parent.addPaintListener(painter);
				labelControl.setData(painter);
			} else {
				return;
			}
		} else {
			if (color == HIGHLIGHT_NONE) {
				parent.removePaintListener((PaintListener) data);
				labelControl.setData(null);
			} else if (color != ((HighlightPainter) data).fColor){
				((HighlightPainter) data).fColor= color;
			} else {
				return;
			}
		}
		
		parent.redraw();
		parent.update();
	}
	
}
