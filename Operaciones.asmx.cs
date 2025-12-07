using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Web.Services;

namespace WebServiceOperaciones
{
    [WebService(Namespace = "http://tempuri.org/")]
    [WebServiceBinding(ConformsTo = WsiProfiles.BasicProfile1_1)]
    [System.ComponentModel.ToolboxItem(false)]
    public class Operaciones : System.Web.Services.WebService
    {
        [WebMethod]
        public int Sumar(int numero1, int numero2)
        {
            return numero1 + numero2;
        }

        [WebMethod]
        public int Restar(int numero1, int numero2)
        {
            return numero1 - numero2;
        }

        [WebMethod]
        public int Multiplicar(int numero1, int numero2)
        {
            return numero1 * numero2;
        }

        [WebMethod]
        public double Dividir(int numero1, int numero2)
        {
            if (numero2 == 0)
                return 0;
            return (double)numero1 / numero2;
        }
    }
}
