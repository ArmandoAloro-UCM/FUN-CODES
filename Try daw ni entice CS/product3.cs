using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;
using System.Data.SqlClient;

namespace APPSDEV_PROJECT
{
    public partial class product3 : Form
    {
        private string userEmail;

        public product3(string email = "")
        {
            InitializeComponent();
            userEmail = email;
        }

        private void button1_Click(object sender, EventArgs e)
        {
            this.Close();
        }
    }
}
