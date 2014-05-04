/*
 * Copyright 2014 Elastic Modules Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

Ext.Loader.setConfig ({
	enabled: true ,
	paths: {
		'Ext.ux.data.proxy': 'bower_components/ext.ux.data.proxy.websocket',
        'Ext.ux': 'bower_components/ext.ux.websocket'
	}
});

Ext.require (['Ext.ux.data.proxy.WebSocket']);

Ext.onReady (function () {
	Ext.define ('model', {
		extend: 'Ext.data.Model' ,
		fields: ['id', 'name', 'age'] ,
		proxy: {
			type: 'websocket' ,
			storeId: 'myStore',
			url: 'ws://localhost:8080' ,
			reader: {
				type: 'json' ,
				root: 'user'
			}
		}
	});
	
	var store = Ext.create ('Ext.data.Store', {
		model: 'model',
		storeId: 'myStore'
	});
	
	//store.proxy.store = store;
	
	var grid = Ext.create ('Ext.grid.Panel', {
		renderTo: Ext.getBody () ,
		title: 'WebSocketed Grid' ,
		width: 500 ,
		height: 300 ,
		store: store ,
		
		selType: 'rowmodel' ,
		selModel: 'rowmodel' ,
		plugins: [Ext.create ('Ext.grid.plugin.CellEditing', {
			clicksToEdit: 1
		})] ,
		
		columns: [{
			xtype: 'rownumberer'
		} , {
			text: 'ID' ,
			dataIndex: 'id' ,
			hidden: true
		} , {
			text: 'Name' ,
			dataIndex: 'name' ,
			flex: 1 ,
			editor: {
				xtype: 'textfield'
			}
		} , {
			text: 'Age' ,
			dataIndex: 'age' ,
			editor: {
				xtype: 'numberfield'
			}
		}] ,
		
		tbar: {
			xtype: 'toolbar' ,
			defaultType: 'button' ,
			items: [{
				text: 'Create' ,
				icon: 'images/plus-circle.png' ,
				handler: function (btn) {
					store.insert (0,{});
				}
			} , '-' , {
				text: 'Read' ,
				icon: 'images/arrow-circle.png' ,
				handler: function (btn) {
					store.load (function (records, operation, success) {
//						console.log (records);
					});
				}
			} , '-' , {
				text: 'Update' ,
				icon: 'images/disk--pencil.png' ,
				handler: function (btn) {
					store.sync ({
						success: function () {
							store.load ();
						}
					});
				}
			} , '-' , {
				text: 'Destroy' ,
				icon: 'images/cross-circle.png' ,
				handler: function (btn) {
					store.remove (grid.getSelectionModel().getSelection ());
				}
			}]
		}
	});
	
	var chart = Ext.create ('Ext.chart.Chart', {
		renderTo: Ext.getBody () ,
		title: 'WebSocketed Chart' ,
		width: 500 ,
		height: 300 ,
		store: store ,
		
		axes: [{
			type: 'Category' ,
			position: 'bottom' ,
			fields: ['name']
		} , {
			type: 'Numeric' ,
			position: 'left' ,
			minimum: 0 ,
			fields: ['age']
		}] ,
		
		series: [{
			type: 'column' ,
			axis: 'left' ,
			xField: 'name' ,
			yField: 'age'
		}]
	});
});
