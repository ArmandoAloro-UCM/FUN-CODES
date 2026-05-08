using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;
using System.Xml.Serialization;

namespace APPSDEV_PROJECT
{
    public partial class Form3 : Form
    {
        private string userEmail; // store email passed from Form2

        public Form3(string email = "")
        {
            InitializeComponent();
            userEmail = email;
        }
        private void btn_product1_Click(object sender, EventArgs e)
        {
            this.Hide();
            product1 p1 = new product1(userEmail); // pass email
            p1.ShowDialog();
            this.Show();
        }

        private void btn_product2_Click(object sender, EventArgs e)
        {
            this.Hide();
            product2 p2 = new product2(userEmail); // pass email
            p2.ShowDialog();
            this.Show();
        }
        private void btn_product3_Click(object sender, EventArgs e)
        {
            this.Hide();
            product3 p3 = new product3(userEmail); // pass email
            p3.ShowDialog();
            this.Show();
        }

        // AYAW NI HILABTI KAI MA GUBA TANAN
        private void Form3_Load(object sender, EventArgs e)
        {
        }

        private void Form3_Load_1(object sender, EventArgs e)
        {
        }
    }
}
