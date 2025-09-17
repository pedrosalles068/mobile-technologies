using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace CivisPlus
{
    public static class AppState
    {
        public static string CodigoIbgeAtual { get; set; }
        public static Stopwatch StartupStopwatch { get; } = new Stopwatch();
    }
}
