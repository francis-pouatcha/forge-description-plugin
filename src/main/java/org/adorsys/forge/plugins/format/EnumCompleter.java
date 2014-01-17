package org.adorsys.forge.plugins.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.forge.shell.completer.SimpleTokenCompleter;

public class EnumCompleter extends SimpleTokenCompleter
{
   private final Class<? extends Enum<?>> type;

   public EnumCompleter(Class<? extends Enum<?>> type)
   {
      this.type = type;
   }

   @Override
   public List<Object> getCompletionTokens()
   {
      List<Object> result = new ArrayList<Object>();
      Enum<?>[] constants = type.getEnumConstants();
      if (constants != null)
      {
         List<Enum<?>> list = Arrays.asList(constants);
         for (Enum<?> e : list)
         {
            result.add(e.toString());
         }
      }
      return result;
   }

}
