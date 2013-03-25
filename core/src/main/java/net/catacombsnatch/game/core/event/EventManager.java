package net.catacombsnatch.game.core.event;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;


/** Manages event handling. */
public class EventManager {
	protected final static Map<Class<? extends Event>, EventRegistry> registry;
	
	static {
		registry = new HashMap<Class<? extends Event>, EventRegistry>();
	}
	
	
	public void registerListener(Object listener) {
		for(Method method : listener.getClass().getMethods()) {
			EventHandler handler = method.getAnnotation(EventHandler.class);
			if(handler == null) continue;
			
			Type returnType = method.getReturnType();
			if(returnType != Void.TYPE) {
				throw new GdxRuntimeException("Unsupported return type: " + returnType.toString());
			}
			
			if(method.getParameterTypes().length != 1) {
				throw new GdxRuntimeException("Method " + method.getName() + " needs exactly 1 parameter!");
			}
			
			Class<?> eventParam = method.getParameterTypes()[0];
			if(eventParam.isAssignableFrom(Event.class)) {
				EventRegistry entry = registry.get((Class<? extends Event>) eventParam);
				
				if(entry == null) {
					entry = new EventRegistry();
					registry.put((Class<? extends Event>) eventParam, entry);
				}
				
				entry.addEntry(handler.priority(), new Listener(handler.ignoreCancelled(), method, listener));
				
			} else {
				throw new GdxRuntimeException("Method " + method.getName() + " does not have a proper event parameter!");
			}
		}
	}
	
	public static void callEvent(Event event) {
		EventRegistry entry = registry.get(event.getClass());
		
		for(EventOrder priority : EventOrder.values()) {
			Array<Listener> listeners = entry.getListeners(priority);
			
			for(Listener listener : listeners) {
				if(event.isCancelled() && !listener.ignoresCancelledEvents()) continue;
				
				listener.listen(event);
			}
		}
	}
	
	protected class EventRegistry {
		protected final Map<EventOrder, Array<Listener>> map;
		
		public EventRegistry() {
			map = new EnumMap<EventOrder, Array<Listener>>(EventOrder.class);
		}
		
		public void addEntry(EventOrder priority, Listener entry) {
			Array<Listener> listeners = map.get(priority);
			listeners.add(entry);
		}
		
		public Array<Listener> getListeners(EventOrder priority) {
			return map.get(priority);
		}
	}
	
	protected class Listener {
		protected final boolean ignores;
		protected final Method method;
		protected final Object instance;
		
		public Listener(boolean ignores, Method method, Object clazz) {
			this.ignores = ignores;
			this.method = method;
			this.instance = clazz;
		}

		public boolean ignoresCancelledEvents() {
			return ignores;
		}
		
		public void listen(Event event) {
			try { method.invoke(instance, event); }
			catch (Exception ex) {}
		}
	}
	
}